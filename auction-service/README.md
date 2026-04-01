# auction-service 분리 작업 기록

모놀리식 bowchat 프로젝트에서 경매 도메인을 독립된 서비스로 분리했다.
product-service와의 연동, 입찰 검증, Kafka 브로드캐스트 구조를 함께 정리했다.

---

## 배경

기존 bowchat은 경매, 상품, 유저 도메인이 같은 JVM 안에서 직접 참조하는 모놀리식 구조였다.
경매 시작 시 상품/판매자 정보를 직접 JOIN으로 가져왔는데, MSA로 분리하면서 서비스 간 검증과 이벤트 발행 구조를 새로 설계했다.

---

## 변경 내용

### 1. 경매 시작 - product-service 연동

경매 시작 시 두 가지를 검증해야 한다.
- 상품이 실제로 존재하는지
- 요청한 유저가 해당 상품의 판매자인지

product-service 내부 API를 FeignClient로 호출해서 한 번에 처리했다.

```java
// AuctionService.startAuction()
Long sellerId;
try {
    sellerId = productServiceClient.getSellerId(productId);
} catch (FeignException.NotFound e) {
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 상품입니다.");
} catch (FeignException e) {
    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "상품 서비스에 접근할 수 없습니다.");
}

if (!sellerId.equals(requestUserId)) {
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "상품 판매자만 경매를 시작할 수 있습니다.");
}

Auction auction = Auction.of(productId, sellerId, request.startingPrice(), request.endTime());
auctionRepository.save(auction);
```

product-service에 내부 전용 엔드포인트를 추가했다:

```java
// product-service InternalProductController
@GetMapping("/internal/products/{productId}/seller")
public ResponseEntity<Long> getSellerId(@PathVariable Long productId) {
    return ResponseEntity.ok(productService.getSellerIdByProductId(productId));
}
```

---

### 2. 입찰 검증 - 로컬 DB만 사용

입찰할 때마다 product-service를 호출하면 부하가 생긴다.
경매 시작 시 `sellerId`를 `Auction` 엔티티에 저장해두고, 입찰 검증 시에는 로컬 DB만 참조한다.

```
경매 시작 → product-service HTTP 호출 → sellerId 검증 + Auction에 저장
입찰 시   → Auction.sellerId로 판매자 입찰 방지 (HTTP 호출 없음)
```

```java
public void validateBid(Long bidderId, Long bidAmount) {
    // 판매자 입찰 방지 - 로컬 DB의 sellerId로 체크
    if (this.sellerId != null && this.sellerId.equals(bidderId)) {
        throw new AuctionException(AuctionErrorCode.SELLER_CANNOT_BID);
    }
    if (bidAmount <= this.currentPrice) {
        throw new AuctionException(AuctionErrorCode.BID_TOO_LOW);
    }
    if (isClosed(LocalDateTime.now())) {
        throw new AuctionException(AuctionErrorCode.AUCTION_CLOSED);
    }
}
```

---

### 3. 낙관적 락으로 동시 입찰 처리

동시에 여러 유저가 입찰하면 같은 경매에 중복 처리가 발생할 수 있다.
`@Version`으로 낙관적 락을 적용해서 동시 입찰 시 하나만 성공하도록 했다.

```java
@Entity
public class Auction {
    @Version
    private Long version;  // 동시 입찰 시 충돌 감지
    ...
}
```

입찰자가 많지 않아 충돌 확률이 낮으므로 비관적 락 대신 낙관적 락을 선택했다.
충돌 시 `OptimisticLockException`이 발생하고 클라이언트가 재시도한다.

---

### 4. 입찰 브로드캐스트 - ChatProducer 연동

입찰 성공 시 경매방 참여자들에게 실시간으로 알려야 한다.
kafka-starter의 `ChatProducer`로 `auction-bid` 토픽에 이벤트를 발행한다.

```java
private void sendBroadcast(Long auctionId, Long bidderId, String bidderNickname, Long bidAmount) {
    EventMessage message = EventMessage.builder()
            .roomId(auctionId)           // auctionId = roomId로 매핑
            .senderId(bidderId)
            .senderName(bidderNickname)
            .topicName(MessageType.AUCTION_BID.getTopicName())
            .messageType(MessageType.AUCTION_BID.name())
            .content(String.valueOf(bidAmount))
            .timestamp(Instant.now().toEpochMilli())
            .build();

    chatProducer.send(message);
}
```

브로드캐스트는 **비동기**로 처리한다. 입찰 자체는 DB에 이미 저장됐으므로 브로드캐스트 실패가 입찰 롤백으로 이어지면 안 된다. 실패 시 DLQ로 적재되고 재처리된다.

입찰자 닉네임은 `UserQueryService`를 통해 Redis → 로컬 DB → user-service HTTP 순으로 조회한다.

---

### 5. sellerId JWT에서 추출

모놀리식에서는 경매 시작 요청에 sellerId를 직접 전달했다.
MSA에서는 JWT 토큰에서 추출하므로 위변조가 불가능하다.

```java
// 기존 - 클라이언트가 sellerId 직접 전달 (위변조 가능)
// 변경 후 - JWT에서 추출
@PostMapping("/{productId}/start")
public ResponseEntity<Void> startAuction(
        @PathVariable Long productId,
        @RequestBody StartAuctionRequest request,
        @AuthenticationPrincipal UserPrincipal user  // JWT에서 userId 추출
) {
    auctionService.startAuction(productId, user.userId(), request);
}
```

입찰도 동일하게 JWT에서 bidderId를 추출한다.

---

### 6. 유저 정보 동기화 - product-service와 동일한 구조

product-service와 동일하게 Redis → 로컬 UserSnapshot → user-service HTTP Lazy 동기화 구조를 적용했다.
캐시 prefix만 다르게 설정해서 Redis 키 충돌을 방지했다.

```java
private static final String CACHE_PREFIX = "auction:user:";
```

```
user-service    → refresh_token:{email}, blacklist:{token}
product-service → product:user:{userId}
auction-service → auction:user:{userId}
```

---

## 전체 구조

```
auction-service (port: 8083)
├── JWT 클레임 파싱 (DB 조회 없음)
├── Redis 블랙리스트 체크
├── 경매 시작: product-service 검증 → Auction 저장
├── 입찰: 로컬 DB 검증 → AuctionBid 저장 → 브로드캐스트
└── user.created 이벤트 수신 → UserSnapshot 저장

경매 시작 흐름:
JWT(sellerId) → product-service HTTP(상품 존재 + 판매자 확인) → Auction 저장

입찰 흐름:
JWT(bidderId) → Auction.validateBid(로컬 DB) → AuctionBid 저장
             → UserQueryService(닉네임 조회) → ChatProducer(브로드캐스트)

내부 API 보안:
Docker Network 격리 + X-Service-Token 헤더 검증
```
