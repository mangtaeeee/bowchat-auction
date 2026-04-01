# product-service 분리 작업 기록

모놀리식 bowchat 프로젝트에서 상품 도메인을 독립된 서비스로 분리했다.
user-service와의 연동 방식, JWT 인증 구조, 유저 정보 동기화 전략을 함께 정리했다.

---

## 배경

기존 bowchat은 상품, 유저, 채팅, 경매 도메인이 같은 JVM 안에서 직접 참조하는 모놀리식 구조였다.
상품 조회 시 판매자 정보를 같은 DB에서 직접 JOIN으로 가져왔는데, MSA로 분리하면서 유저 정보를 어떻게 가져올지가 핵심 문제였다.

---

## 변경 내용

### 1. JWT 인증 구조 - DB 조회 없는 경량 인증

user-service와 달리 product-service는 자체 User DB가 없다.
JWT 클레임에 `userId`, `nickname`, `role`이 포함되어 있으므로 DB 조회 없이 클레임만 파싱해서 인증 객체를 만든다.

```java
// user-service JwtProvider - DB 조회
public Authentication getAuthentication(String token) {
    Long userId = claims.get("userId", Long.class);
    User user = userRepository.findById(userId).orElseThrow(...); // DB 조회
    ...
}

// product-service JwtProvider - 클레임만 파싱
public Authentication getAuthentication(String token) {
    Long userId     = claims.get("userId", Long.class);
    String email    = claims.getSubject();
    String nickname = claims.get("nickname", String.class);
    String role     = claims.get("role", String.class);

    UserPrincipal principal = new UserPrincipal(userId, email, nickname, role);
    return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
}
```

`UserPrincipal`은 record로 구현해서 불변성을 보장했다.
Controller에서 `@AuthenticationPrincipal UserPrincipal user`로 꺼내서 `user.userId()`로 바로 접근한다.

---

### 2. sellerId DTO 제거

모놀리식에서는 클라이언트가 `sellerId`를 직접 전달했다.
MSA에서는 JWT 토큰에서 추출하므로 DTO에서 제거했다.

**기존**
```java
public record ProductCreateDTO(
        String name,
        String description,
        Long price,
        List<String> imageUrls,
        SaleType saleType,
        Long sellerId  // 클라이언트가 직접 전달 → 위변조 가능
) {}
```

**변경 후**
```java
public record ProductCreateDTO(
        String name,
        String description,
        Long price,
        List<String> imageUrls,
        SaleType saleType
        // sellerId 제거 - JWT에서 추출
) {}
```

```java
@PostMapping
public ResponseEntity<Long> addProduct(
        @RequestBody ProductCreateDTO dto,
        @AuthenticationPrincipal UserPrincipal user  // JWT에서 추출
) {
    Long productId = productService.addProduct(dto, user.userId());
    return ResponseEntity.ok(productId);
}
```

---

### 3. Redis 블랙리스트 체크

user-service에서 로그아웃 시 Redis에 블랙리스트를 등록한다.
product-service의 `JwtAuthenticationFilter`에서도 동일하게 블랙리스트를 체크해서 로그아웃된 토큰을 차단한다.

```java
if (token != null && jwtProvider.validateToken(token)) {
    if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token))) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"message\":\"로그아웃된 토큰입니다.\"}");
        return;
    }
    SecurityContextHolder.getContext().setAuthentication(jwtProvider.getAuthentication(token));
}
```

Redis database를 user-service(0)와 product-service(1)로 분리해서 키 충돌을 방지했다.
단, 블랙리스트는 두 서비스가 같은 Redis database를 참조해야 동작한다. 현재는 같은 Redis 인스턴스를 사용하므로 database 번호를 맞춰줘야 한다.

---

### 4. 유저 정보 동기화 전략

상품 상세 조회 시 판매자 닉네임이 필요하다.
user-service DB를 직접 참조할 수 없으므로 세 가지 계층으로 유저 정보를 조회한다.

```
Redis 캐시 (TTL 10분)
    ↓ miss
로컬 DB UserSnapshot
    ↓ miss
user-service HTTP 호출 (Lazy 동기화)
```

```java
public UserSnapshot getUser(Long userId) {
    // 1. Redis 캐시
    Object cached = redisTemplate.opsForValue().get("user:" + userId);
    if (cached != null) return objectMapper.convertValue(cached, UserSnapshot.class);

    // 2. 로컬 DB
    UserSnapshot local = userSnapshotRepository.findById(userId).orElse(null);
    if (local != null) {
        redisTemplate.opsForValue().set("user:" + userId, local, TTL);
        return local;
    }

    // 3. user-service HTTP 호출
    UserSnapshot snapshot = userServiceClient.getUser(userId);
    userSnapshotSaver.save(snapshot);  // 별도 트랜잭션
    redisTemplate.opsForValue().set("user:" + userId, snapshot, TTL);
    return snapshot;
}
```

Redis에서 꺼낸 값은 `Jackson2JsonRedisSerializer`가 `LinkedHashMap`으로 역직렬화하므로
`ObjectMapper.convertValue()`로 `UserSnapshot`으로 변환한다.

---

### 5. 트랜잭션 경계 분리

외부 HTTP 호출(FeignClient)과 DB 저장을 같은 트랜잭션에 묶으면 HTTP 호출 동안 DB 커넥션을 점유해 커넥션 풀이 고갈될 수 있다.

`UserSnapshotSaver`를 별도 빈으로 분리해서 HTTP 호출과 트랜잭션을 분리했다.

```java
@Component
public class UserSnapshotSaver {

    // UserQueryService에서 HTTP fallback 후 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(UserSnapshot snapshot) {
        userSnapshotRepository.save(snapshot);
    }

    // Kafka 이벤트 수신 시 멱등성 보장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveIfAbsent(UserSnapshot snapshot) {
        if (!userSnapshotRepository.existsById(snapshot.getUserId())) {
            userSnapshotRepository.save(snapshot);
        }
    }
}
```

`REQUIRES_NEW`를 사용해 외부 트랜잭션(readOnly 포함)과 항상 독립적으로 실행된다.

---

### 6. Kafka 이벤트 기반 UserSnapshot 동기화

user-service가 `user.created` 이벤트를 발행하면 product-service가 수신해서 `UserSnapshot`을 로컬에 저장한다.

```java
@KafkaListener(topics = "user.created", groupId = "product-service-group")
public void handleUserCreated(String message) {
    try {
        // user-service OutboxScheduler가 String payload를 JsonSerializer로 발행해서 이중 인코딩됨
        // 바깥 따옴표를 먼저 벗겨낸 후 파싱
        String innerJson = objectMapper.readValue(message, String.class);
        UserCreatedEvent event = objectMapper.readValue(innerJson, UserCreatedEvent.class);

        userSnapshotSaver.saveIfAbsent(UserSnapshot.builder()
                .userId(event.userId())
                .email(event.email())
                .nickname(event.nickName())
                .build());
    } catch (Exception e) {
        log.error("user.created 처리 실패: {}", message, e);
        throw new RuntimeException(e);  // DLQ로 전달
    }
}
```

`saveIfAbsent()`는 `REQUIRES_NEW` 트랜잭션 안에서 조회+저장을 원자적으로 처리해 멀티 인스턴스 환경에서 중복 저장을 방지한다.

---

### 7. 내부 서비스 API 인증 - FeignClient + X-Service-Token

user-service 내부 API 호출 시 `X-Service-Token` 헤더를 자동으로 추가한다.

```java
@Configuration
public class FeignConfig {

    @Value("${internal.secret}")
    private String internalSecret;

    @Bean
    public RequestInterceptor internalTokenInterceptor() {
        return requestTemplate ->
                requestTemplate.header("X-Service-Token", internalSecret);
    }
}
```

```java
@FeignClient(name = "user-service", url = "${user-service.url}")
public interface UserServiceClient {

    @GetMapping("/internal/users/{userId}")
    UserSnapshot getUser(@PathVariable Long userId);
}
```

FeignClient 예외 처리:
- `FeignException.NotFound` → 404 (존재하지 않는 회원)
- `FeignException` → 503 (user-service 장애, 서비스 불가)

---

## 전체 구조

```
product-service (port: 8082)
├── JWT 클레임 파싱 (DB 조회 없음)
├── Redis 블랙리스트 체크
├── 상품 등록/조회 API
└── user.created 이벤트 수신 → UserSnapshot 저장

유저 정보 조회 흐름:
Redis (TTL 10분) → 로컬 UserSnapshot → user-service HTTP (Lazy 동기화)

내부 API 보안:
Docker Network 격리 + X-Service-Token 헤더 검증
```
