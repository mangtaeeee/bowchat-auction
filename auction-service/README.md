# auction-service 분리 작업 기록

모놀리식 `bowchat` 프로젝트에서 경매 도메인을 독립 서비스로 분리했다.  
이 서비스는 `product-service`, `user-service`, `chat-service`와 연동하며, 특히 입찰 정합성과 이벤트 발행 정합성을 핵심 책임으로 둔다.

---

## 배경

기존 구조에서는 경매, 상품, 사용자 도메인이 같은 JVM 안에서 직접 참조됐다.  
MSA로 분리하면서 다음 문제를 새로 설계해야 했다.

- 경매 시작 시 상품 존재 여부와 판매자 검증을 어떻게 할지
- 입찰 시 외부 서비스 호출 없이 판매자 입찰 금지를 어떻게 처리할지
- 동시 입찰에서 가격 정합성을 어떻게 보장할지
- 입찰 성공 후 채팅방에 실시간으로 어떻게 전달할지

---

## 핵심 설계

### 1. 경매 시작 시 product-service 검증

경매 시작은 두 가지를 확인해야 한다.

- 상품이 실제로 존재하는지
- 요청한 사용자가 해당 상품의 판매자인지

이를 위해 `product-service`의 내부 API를 Feign으로 호출한다.

관련 코드:
- [AuctionService.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/service/AuctionService.java)
- [ProductServiceClient.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/client/ProductServiceClient.java)

### 2. 입찰 검증은 로컬 Auction 데이터 기준

입찰마다 `product-service`를 다시 호출하면 불필요한 네트워크 비용과 장애 전파가 생긴다.  
그래서 경매 시작 시 검증한 `sellerId`를 `Auction` 엔티티에 함께 저장하고, 입찰 시에는 로컬 DB만 본다.

이 방식으로 처리하는 규칙:

- 판매자는 자신의 상품에 입찰할 수 없음
- 현재가 이하 금액은 입찰할 수 없음
- 종료된 경매에는 입찰할 수 없음

관련 코드:
- [Auction.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/entity/Auction.java)
- [AuctionBidService.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/service/AuctionBidService.java)

### 3. JWT에서 사용자 식별

경매 시작과 입찰 요청 모두 `userId`를 요청 본문에서 받지 않는다.  
인증된 사용자 정보는 JWT에서 추출해 사용한다.

이 방식으로 다음 문제를 막는다.

- 클라이언트가 임의의 `sellerId`, `bidderId`를 보내는 위조 가능성
- 요청 DTO에 인증 정보가 섞이는 문제

관련 코드:
- [AuctionController.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/controller/AuctionController.java)
- [JwtProvider.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/auth/JwtProvider.java)
- [JwtAuthenticationFilter.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/auth/filter/JwtAuthenticationFilter.java)

### 4. 입찰 성공 후 Outbox 패턴으로 이벤트 발행 정합성 확보

처음에는 입찰이 성공하면 같은 흐름 안에서 바로 Kafka를 발행하는 구조였다.  
하지만 이 방식은 DB 저장은 성공했는데 Kafka 발행 전에 장애가 나면, 입찰은 반영됐지만 이벤트는 유실되는 문제가 생길 수 있다.

그래서 지금은 다음 구조로 바꿨다.

```text
입찰 저장 + 입찰 이력 저장 + Outbox 저장 (같은 트랜잭션)
    -> OutboxRelayScheduler polling
    -> Kafka 발행 성공 후 PUBLISHED 처리
    -> 실패 시 다시 PENDING 으로 돌려 재시도
```

즉 입찰 저장과 Kafka 발행을 한 트랜잭션으로 묶은 것이 아니라,  
입찰 저장과 Outbox 기록을 같은 로컬 트랜잭션으로 묶어 이벤트 유실 없이 후속 발행을 재시도할 수 있게 했다.

관련 코드:
- [AuctionBidService.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/service/AuctionBidService.java)
- [AuctionOutboxService.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/outbox/AuctionOutboxService.java)
- [OutboxEvent.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/outbox/OutboxEvent.java)
- [OutboxEventRepository.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/outbox/OutboxEventRepository.java)
- [OutboxRelayScheduler.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/outbox/OutboxRelayScheduler.java)

### 5. 입찰 성공 응답에 최신 경매 상태 반환

브로드캐스트는 비동기로 처리되므로, 입찰한 본인은 WebSocket 이벤트를 기다리지 않고도 화면을 바로 갱신할 수 있어야 한다.  
그래서 입찰 API는 `200 OK`만 내려주지 않고, 성공 시 최신 `AuctionResponse`를 함께 반환한다.

이렇게 하면 프론트는:

1. 입찰 요청 성공 응답으로 본인 화면을 즉시 갱신하고
2. 이후 WebSocket/채팅 이벤트가 오면 서버 기준 상태로 다시 동기화할 수 있다

관련 코드:
- [AuctionController.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/controller/AuctionController.java)
- [AuctionService.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/service/AuctionService.java)
- [AuctionResponse.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/dto/response/AuctionResponse.java)

### 6. 사용자 정보는 UserSnapshot + Redis 캐시

다른 서비스가 `user-service` DB를 직접 보지 않도록, `user.created` 이벤트를 받아 로컬 `UserSnapshot`을 유지한다.  
이후 필요한 경우 다음 순서로 사용자 정보를 조회한다.

1. Redis 캐시
2. 로컬 `UserSnapshot`
3. `user-service` HTTP fallback

관련 코드:
- [UserQueryService.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/user/service/UserQueryService.java)
- [UserSnapshotSaver.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/user/service/UserSnapshotSaver.java)
- [UserCreatedEventProcessor.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/user/service/UserCreatedEventProcessor.java)

---

## 동시 입찰 정합성

### 선택한 방식

같은 경매에 여러 사용자가 동시에 입찰할 수 있으므로 `@Version` 기반 낙관적 락을 사용했다.

```java
@Version
private Long version;
```

이 방식을 선택한 이유:

- 입찰은 읽기 대비 쓰기 충돌이 아주 자주 일어나진 않음
- 비관적 락보다 트랜잭션 점유 비용이 작음
- 충돌 시 한 요청만 성공시키고 나머지는 재시도하게 만드는 구조가 경매 도메인과 잘 맞음

### 충돌 처리 방식

JPA의 `ObjectOptimisticLockingFailureException`을 그대로 밖으로 노출하지 않고,  
도메인 의미가 있는 `CONCURRENT_BID_CONFLICT` 예외로 번역했다.

즉 사용자 입장에서는:

- 단순 서버 오류가 아니라
- "동시에 더 높은 입찰이 반영됐다. 다시 시도해라"

라는 의미로 이해할 수 있다.

관련 코드:
- [Auction.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/entity/Auction.java)
- [AuctionBidService.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/service/AuctionBidService.java)
- [AuctionErrorCode.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/entity/AuctionErrorCode.java)

---

## 예외 처리 방식

서비스 계층에서 `ResponseStatusException`을 직접 던지지 않고,  
`AuctionException + AuctionErrorCode`로 예외를 통일했다.

장점:

- 서비스는 비즈니스 의미만 표현
- HTTP 응답 변환은 `GlobalExceptionHandler`가 전담
- 에러 코드와 메시지를 서비스 전반에서 일관되게 유지 가능

관련 코드:
- [AuctionException.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/entity/AuctionException.java)
- [AuctionErrorCode.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/entity/AuctionErrorCode.java)
- [GlobalExceptionHandler.java](C:/Users/김태윤/study/auction-service/src/main/java/com/example/auctionservice/exception/GlobalExceptionHandler.java)

---

## 테스트 전략

동시 입찰은 설명만으로 설득력이 약하므로 테스트를 계층별로 분리했다.

### 1. 도메인 규칙 테스트

`Auction` 엔티티 자체가 다음 규칙을 지키는지 검증한다.

- 판매자 입찰 금지
- 현재가 이하 입찰 금지
- 종료 경매 입찰 금지
- 정상 입찰 시 현재가/낙찰자 갱신

관련 코드:
- [AuctionTest.java](C:/Users/김태윤/study/auction-service/src/test/java/com/example/auctionservice/entity/AuctionTest.java)

### 2. 서비스 orchestration 테스트

`AuctionService`가 다음 흐름을 올바르게 조합하는지 검증한다.

- 판매자만 경매 시작 가능
- 입찰 성공 시 최신 경매 상태 반환

관련 코드:
- [AuctionServiceTest.java](C:/Users/김태윤/study/auction-service/src/test/java/com/example/auctionservice/service/AuctionServiceTest.java)

### 3. 낙관적 락 예외 번역 테스트

`AuctionBidService`가 JPA 낙관적 락 예외를  
`CONCURRENT_BID_CONFLICT`로 바꾸는지 검증한다.

관련 코드:
- [AuctionBidServiceTest.java](C:/Users/김태윤/study/auction-service/src/test/java/com/example/auctionservice/service/AuctionBidServiceTest.java)

### 4. JPA 낙관적 락 통합 테스트

같은 경매를 서로 다른 트랜잭션에서 읽고 저장할 때,  
먼저 반영된 입찰 이후의 stale 엔티티가 실제로 실패하는지 검증한다.

즉 `@Version`이 "붙어 있기만 한 것"이 아니라 실제로 충돌을 막는지 확인한다.

관련 코드:
- [AuctionRepositoryLockTest.java](C:/Users/김태윤/study/auction-service/src/test/java/com/example/auctionservice/repository/AuctionRepositoryLockTest.java)
- [src/test/resources/application.yaml](C:/Users/김태윤/study/auction-service/src/test/resources/application.yaml)

### 5. HTTP 응답 규약 테스트

컨트롤러 레벨에서 다음 응답 규약을 검증한다.

- `401` 인증 실패
- `400` validation 실패
- `403` 판매자 권한 실패
- `404` 조회 대상 없음
- `200` 정상 조회

관련 코드:
- [AuctionControllerWebMvcTest.java](C:/Users/김태윤/study/auction-service/src/test/java/com/example/auctionservice/controller/AuctionControllerWebMvcTest.java)

### 6. Outbox 저장/발행 테스트

입찰 저장과 Outbox 기록이 같은 트랜잭션 경계에서 처리되는지,  
스케줄러가 발행 성공/실패에 따라 상태를 올바르게 바꾸는지 검증한다.

관련 코드:
- [AuctionBidServiceTest.java](C:/Users/김태윤/study/auction-service/src/test/java/com/example/auctionservice/service/AuctionBidServiceTest.java)
- [OutboxRelaySchedulerTest.java](C:/Users/김태윤/study/auction-service/src/test/java/com/example/auctionservice/outbox/OutboxRelaySchedulerTest.java)

### 7. UserSnapshot 저장 경로 테스트

중복 이벤트 소비를 견디기 위해 `existsById()`가 아니라  
`ON CONFLICT` 기반 저장 경로를 타는지 확인한다.

관련 코드:
- [UserSnapshotSaverTest.java](C:/Users/김태윤/study/auction-service/src/test/java/com/example/auctionservice/user/service/UserSnapshotSaverTest.java)

---

## 트레이드오프

- 입찰 충돌 처리에는 비관적 락 대신 낙관적 락을 사용했다.
- 충돌 시 자동 재시도 대신 `409`로 응답하고 클라이언트 재시도를 전제로 했다.
- 사용자 정보는 강한 동기화 대신 `UserSnapshot + Redis` 기반 eventual consistency를 허용했다.
- 이벤트 발행은 즉시 Kafka 호출 대신 polling Outbox를 사용해 구현 복잡도를 낮췄다.
- 브로드캐스트는 비동기 처리되므로, 입찰한 본인 화면은 HTTP 응답의 최신 상태로 먼저 갱신하는 전제를 둔다.

---

## 전체 흐름

```text
경매 시작:
JWT(userId)
-> product-service 내부 API로 상품/판매자 검증
-> Auction 저장

입찰:
JWT(userId)
-> Auction.validateBid()
-> Auction 저장(@Version 기반 낙관적 락)
-> AuctionBid 히스토리 저장
-> Outbox 이벤트 저장 (같은 트랜잭션)
-> 입찰 성공 응답으로 최신 AuctionResponse 반환
-> OutboxRelayScheduler polling
-> ChatProducer로 auction-bid 이벤트 발행
```
