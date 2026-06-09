# user-service 분리 작업 기록

모놀리식 bowchat 프로젝트에서 유저 도메인을 독립된 서비스로 분리했다.
단순히 패키지를 옮기는 수준이 아니라 MSA 환경에서 실제로 동작하도록 여러 구조적 문제를 함께 해결했다.

---

## 배경

기존 bowchat은 단일 Spring Boot 애플리케이션 안에 유저, 채팅, 경매, 상품 도메인이 전부 공존하는 모놀리식 구조였다.
서비스가 커지면서 도메인 간 결합도가 높아졌고, 유저 정보에 의존하는 코드가 여러 도메인에 산재해 있었다.
이번 작업에서는 유저 도메인을 user-service로 분리하고, 다른 서비스와의 통신 방식을 정리했다.

---

## 변경 내용

### 1. JWT 토큰 클레임 구조 통일

모놀리식에서는 로컬 로그인과 SNS 로그인의 토큰 발급 경로가 달랐다.

**기존**
```java
// 로컬 로그인 - email만 담음
public String generateToken(Authentication authentication) {
    PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
    return Jwts.builder()
            .setSubject(principalDetails.getUsername())
            .setExpiration(...)
            .signWith(getSigningKey())
            .compact();
}

// SNS 로그인 - userId, provider 포함
public String generateToken(User user) {
    return Jwts.builder()
            .setSubject(user.getEmail())
            .claim("userId", user.getId())
            .claim("provider", user.getProvider().name())
            ...
}
```

로컬 로그인 토큰에는 `userId`, `nickname`, `role` 클레임이 없었고 SNS 로그인과 구조가 달랐다.
다른 서비스에서 토큰을 파싱할 때 로컬/SNS 분기 처리가 필요한 구조였다.

**변경 후**
```java
// 로컬/SNS 모두 동일한 메서드로 통일
public String generateToken(User user) {
    return Jwts.builder()
            .setSubject(user.getEmail())
            .claim("userId", user.getId())
            .claim("nickname", user.getNickname())
            .claim("role", user.getRole().name())
            .setExpiration(...)
            .signWith(getSigningKey())
            .compact();
}
```

`Authentication`을 받는 오버로드 메서드를 제거하고 `User` 객체 기반으로 단일화했다.
다른 서비스에서 토큰을 파싱할 때 DB 조회 없이 클레임에서 `userId`, `nickname`, `role`을 바로 꺼낼 수 있게 됐다.

---

### 2. 토큰 인증 조회 방식 변경

**기존**
```java
public Authentication getAuthentication(String token) {
    Claims claims = parseClaims(token);
    String email = claims.getSubject();
    String providerName = claims.get("provider", String.class);

    ProviderType provider = (providerName == null)
            ? ProviderType.LOCAL
            : ProviderType.valueOf(providerName);

    User user = userRepository.findByEmailAndProvider(email, provider)
            .orElseThrow(...);
    ...
}
```

`provider` 클레임 존재 여부를 분기 처리해야 했고, email + provider 복합 조건 조회라 PK 조회보다 느렸다.

**변경 후**
```java
public Authentication getAuthentication(String token) {
    Claims claims = parseClaims(token);
    Long userId = claims.get("userId", Long.class);

    User user = userRepository.findById(userId)
            .orElseThrow(...);

    PrincipalDetails principal = new PrincipalDetails(user);
    return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
}
```

토큰 발급 시 `userId`를 클레임에 포함시켰기 때문에 가능한 변경이다.
`provider` 클레임도 더 이상 필요 없어서 토큰 구조가 단순해졌다.

현재는 Keycloak access token 안에 `userId`, `nickname`, `role` claim을 넣고,
각 서비스가 `oauth2ResourceServer()`로 JWT를 검증한 뒤 기존 `UserPrincipal` 형태로 변환해서 사용한다.

---

### 3. SecurityConfig 역할 정리

모놀리식 SecurityConfig에는 채팅 WebSocket, 파일 업로드, 템플릿 뷰 등 다른 도메인의 관심사가 섞여 있었다.

**기존**
```java
.requestMatchers("/auth/**", "/user/signup", "/uploads/**",
                         "/oauth2/**", "/view/**", "/h2-console/**").permitAll()
.requestMatchers("/ws/**").permitAll()  // chat-service 관심사
```

**변경 후**

user-service는 인증 API만 담당하므로 관련 엔드포인트만 남겼다.
WebSocket handshake 인터셉터(`JwtHandshakeInterceptor`)도 user-service에서 제거하고 chat-service로 이동했다.
예외 처리도 API 서버답게 항상 JSON으로 응답하도록 단순화했다.

```java
.requestMatchers(
        "/auth/**",
        "/user/signup",
        "/oauth2/**",
        "/actuator/**"
).permitAll()
```

`/internal/**`는 일반 API 체인에서 `permitAll()`로 열어두지 않는다.
별도 SecurityFilterChain에서 OAuth2 Resource Server로 분리해서 검증한다.

---

### 4. 회원가입 이벤트 발행 - 아웃박스 패턴 적용

#### 왜 직접 Kafka 발행이 문제인가

처음에는 회원가입 트랜잭션 안에서 Kafka를 직접 발행하는 방식으로 구현했다.

```java
@Transactional
public void signup(SingUpRequest request) {
    User user = userRepository.save(...);           // DB 트랜잭션 안
    kafkaTemplate.send("user.created", event);      // 트랜잭션 밖
}
```

이 구조의 문제는 DB 저장과 Kafka 발행이 원자적으로 묶이지 않는다는 점이다.
DB 저장은 성공했는데 Kafka 발행이 실패하면 다른 서비스는 해당 유저의 생성 사실을 영원히 알 수 없다.
반대로 Kafka 발행은 됐는데 DB 트랜잭션이 롤백되면 존재하지 않는 유저 이벤트가 발행된다.

#### 아웃박스 패턴으로 해결

Kafka를 직접 발행하는 대신 같은 트랜잭션 안에서 `outbox_events` 테이블에 이벤트를 저장한다.
별도 스케줄러가 주기적으로 이 테이블을 읽어서 Kafka에 발행하고, 성공하면 발행 완료로 표시한다.

```
[기존]
DB 저장 (트랜잭션) → Kafka 발행 (트랜잭션 밖) → 불일치 가능

[아웃박스 패턴]
DB 저장 + outbox 저장 (같은 트랜잭션) → 스케줄러가 Kafka 발행
→ 둘 다 성공하거나 둘 다 실패 → 항상 일치
```

```java
@Transactional
public void signup(SingUpRequest request) {
    User user = userRepository.save(...);

    // Kafka 직접 발행 대신 같은 트랜잭션 안에서 outbox 테이블에 저장
    outboxEventPublisher.saveUserCreatedEvent(UserCreatedEvent.of(user));
}
```

```java
// OutboxScheduler - 1초마다 실행
@Scheduled(fixedDelay = 1000)
@Transactional
public void publishPendingEvents() {
    List<OutboxEvent> pendingEvents =
            outboxRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();

    for (OutboxEvent event : pendingEvents) {
        try {
            kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), event.getPayload());
            event.markPublished();
        } catch (Exception e) {
            log.error("outbox 발행 실패: id={}", event.getId());
        }
    }
}
```

#### ShedLock으로 중복 발행 방지

서버가 여러 대 뜨는 환경에서는 스케줄러가 동시에 실행되면 같은 이벤트가 중복 발행될 수 있다.
ShedLock을 붙여서 동시에 하나의 인스턴스만 스케줄러를 실행하도록 보장했다.

```java
@Scheduled(fixedDelay = 1000)
@SchedulerLock(
        name = "outbox-publisher",
        lockAtLeastFor = "500ms",
        lockAtMostFor = "10s"
)
@Transactional
public void publishPendingEvents() { ... }
```

ShedLock은 DB에 락 테이블을 만들어서 분산 락을 구현한다.
스케줄러가 실행되면 `shedlock` 테이블에 레코드를 잡고, 다른 인스턴스는 해당 레코드가 풀릴 때까지 실행하지 않는다.

---

### 5. OAuth2 회원가입에도 동일하게 적용

로컬 회원가입뿐 아니라 Google, Kakao 등 SNS 로그인으로 신규 유저가 생성될 때도 아웃박스를 통해 이벤트를 발행한다.

```java
private User registerNewUser(ProviderType provider, OAuth2UserInfo oAuth2UserInfo) {
    User savedUser = userRepository.save(newUser);

    // 아웃박스를 통해 이벤트 저장
    outboxEventPublisher.saveUserCreatedEvent(UserCreatedEvent.of(savedUser));

    return savedUser;
}
```

기존 유저가 재로그인하는 경우에는 이벤트를 발행하지 않는다.
이미 다른 서비스에 해당 유저의 스냅샷이 존재하기 때문이다.

이벤트에 `password`는 포함하지 않았다. Kafka 메시지는 여러 서비스가 수신하므로 민감 정보를 포함하면 안 된다.

```java
public record UserCreatedEvent(
        Long userId,
        String email,
        String nickName   // password 제외
) { ... }
```

---

### 6. JWT 토큰 탈취 대응 - 블랙리스트 적용

JWT는 stateless 구조라 서버가 발급한 토큰을 만료 전에 강제로 무효화할 수 없다는 문제가 있다.
토큰이 탈취되거나 사용자가 로그아웃해도 만료 시간까지 해당 토큰으로 API 접근이 가능하다.

이를 해결하기 위해 로그아웃 시 Redis에 블랙리스트를 등록하고, 각 요청마다 필터에서 블랙리스트를 확인하는 방식을 적용했다.

```java
// 로그아웃 시
public void logout(String token, Authentication authentication) {
    // 1. Refresh Token 삭제
    refreshTokenService.delete(resolveAuthenticatedEmail(authentication));

    // 2. Access Token을 남은 만료시간만큼 블랙리스트에 등록
    long expiration = resolveExpirationMillis(authentication);
    if (expiration > 0) {
        redisTemplate.opsForValue().set(
                "blacklist:" + token,
                "logout",
                Duration.ofMillis(expiration)
        );
    }
}
```

```java
// AccessTokenBlacklistFilter - JWT 검증 전에 blacklist를 먼저 확인
if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
    String token = authorizationHeader.substring(7);
    if (redisTemplate.hasKey("blacklist:" + token)) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"message\":\"로그아웃된 토큰입니다.\"}");
        return;
    }
}
```

블랙리스트 TTL을 Access Token의 남은 만료시간으로 설정해서 토큰이 만료되면 Redis에서도 자동으로 삭제된다.
Redis를 이미 Refresh Token 저장에 사용하고 있어서 별도 인프라 추가 없이 적용했다.

---

### 7. 내부 서비스 API 보안 - Keycloak Client Credentials 전환

다른 서비스가 user-service의 유저 정보를 조회할 수 있는 내부 API를 추가했다.
초기에는 공유 시크릿 기반 `X-Service-Token` 방식으로 보호했지만, 서비스가 늘어나면서 시크릿 분배와 회전 비용이 커지고 권한 관리가 분산된다는 문제가 있었다.

그래서 현재는 Keycloak `client_credentials` 기반 Bearer Token으로 내부 API 인증을 통일했다.

핵심 변경점:

- `user-service`, `auction-service`, `chat-service`, `product-service` 모두 내부 API 수신 시 `X-Service-Token` fallback 제거
- 내부 호출은 Feign interceptor에서 Keycloak access token을 발급받아 `Authorization: Bearer ...` 헤더만 전송
- `/internal/**`는 issuer 기반 JWT 검증 후 `scope` 또는 `role`이 맞는 경우에만 허용

```java
@Bean
@Order(1)
public SecurityFilterChain internalFilterChain(HttpSecurity http, ObjectProvider<JwtDecoder> jwtDecoderProvider) throws Exception {
    http
            .securityMatcher("/internal/**")
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .anyRequest().hasAnyAuthority(
                            "ROLE_INTERNAL_SERVICE",
                            "SCOPE_user.internal.read"
                    )
            );

    JwtDecoder jwtDecoder = jwtDecoderProvider.getIfAvailable();
    if (jwtDecoder != null) {
        http.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.decoder(jwtDecoder)));
    }

    return http.build();
}
```

다른 서비스는 Keycloak에서 access token을 받아 내부 API를 호출한다.

```java
// FeignConfig - 모든 내부 FeignClient 요청에 Bearer Token 자동 추가
@Bean
public RequestInterceptor internalAuthenticationInterceptor(
        OAuth2AuthorizedClientManager authorizedClientManager,
        OAuth2InternalClientProperties properties
) {
    return requestTemplate -> {
        String token = OAuth2ClientConfig.resolveAccessToken(
                authorizedClientManager,
                properties.getRegistrationId()
        );
        requestTemplate.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    };
}
```

이 구조로 바꾸면서 `INTERNAL_SECRET` 환경변수는 전체 서비스에서 제거할 수 있게 됐다.

---

### 8. Keycloak을 도입한 이유와 얻은 효과

내부 서비스 인증을 Keycloak으로 옮긴 이유는 단순히 기술 스택을 바꾸기 위해서가 아니라, MSA에서 인증 책임을 중앙화하기 위해서다.

개선된 점:

- 공유 시크릿 배포 제거: 서비스마다 같은 `INTERNAL_SECRET`를 넣어둘 필요가 없어졌다.
- 권한 관리 중앙화: 어떤 서비스가 어떤 내부 API를 호출할 수 있는지 `scope`, `role`로 통제할 수 있다.
- 키 회전 단순화: 각 서비스가 비밀키를 공유하지 않고 issuer 공개키로 검증한다.
- 서비스 확장성 개선: 내부 서비스가 늘어나도 Keycloak client만 추가하면 된다.
- 추적성 향상: 어떤 client가 어떤 토큰으로 호출했는지 토큰 클레임 기준으로 해석 가능하다.

현재 적용된 상태:

- 내부 서비스 간 인증은 Keycloak `client_credentials` 기반으로 동작한다.
- 일반 사용자 로그인과 리프레시 세션도 Keycloak이 검증한다.
- `auction-service`, `chat-service`, `product-service`의 일반 API 인증 체인도 Keycloak JWT 검증으로 전환했다.
- 각 서비스는 Keycloak access token을 기존 `UserPrincipal(userId, email, nickname, role)` 형태로 변환해서 사용한다.
- 로그아웃된 access token은 Redis blacklist로 즉시 무효화한다.
- Google/Kakao 소셜 로그인도 이제 Keycloak 브로커 경유를 기준으로 동작한다.

구체적인 적용 방식:

1. 로그인
   - 로컬 계정 비밀번호를 앱에서 1차 검증
   - Keycloak 사용자와 비밀번호를 동기화
   - Keycloak token endpoint로 `password grant` 인증
   - 성공 시 Keycloak `access token`, `refresh token`을 그대로 사용한다

2. 리프레시
   - 브라우저 쿠키에 담긴 Keycloak `refresh token`으로 Keycloak에 재발급 요청
   - 회전된 refresh token을 Redis와 쿠키에 다시 저장
   - 새 Keycloak access token을 그대로 반환

3. 로그아웃
   - Keycloak logout endpoint 호출
   - Redis에 저장된 refresh token 삭제
   - 현재 Keycloak access token은 남은 만료시간만큼 Redis blacklist 처리

향후 최종 목표:

1. 내부 서비스 간 인증은 전부 Keycloak `client_credentials`로 통일
2. 소셜 로그인도 애플리케이션 직접 연동 대신 Keycloak 브로커로 수렴
3. Keycloak claim mapper와 role 체계를 리소스 기준으로 정리
4. 각 서비스는 토큰 발급이 아니라 검증과 인가에만 집중

---

### 9. 사용자 로그인/리프레시의 Keycloak 연동

이번 단계에서 추가된 핵심은 `최종 사용자 access token`까지 Keycloak JWT로 통일했다는 점이다.

#### 적용 전제

다른 서비스 컨트롤러는 이미 `UserPrincipal(userId, email, nickname, role)`를 전제로 작성돼 있다.
그래서 Keycloak access token도 최소한 아래 claim 계약을 만족하도록 맞췄다.

```text
userId
email
nickname
role
```

실제 검증은 Keycloak 공개키 기반으로 수행하고, 각 서비스는 JWT를 `UserPrincipal`로 변환한다.
즉 토큰 발급자는 Keycloak이고, 애플리케이션은 claim 해석과 인가만 담당한다.

#### 로그인 흐름

```text
1. /auth/login
2. user-service가 로컬 계정 비밀번호 검증
3. Keycloak admin API로 사용자 속성/비밀번호 동기화
4. Keycloak token endpoint(password grant) 호출
5. Keycloak refresh token 저장
6. Keycloak access token 반환
7. 브라우저에는 refresh token 쿠키 저장
```

#### 리프레시 흐름

```text
1. 브라우저 쿠키에서 refresh token 추출
2. Redis에서 현재 세션과 일치하는지 확인
3. Keycloak token endpoint(refresh_token grant) 호출
4. 회전된 refresh token을 Redis와 쿠키에 갱신
5. 새 Keycloak access token 반환
```

#### 로그아웃 흐름

```text
1. Redis에서 refresh token 조회
2. Keycloak logout endpoint 호출
3. Redis refresh token 삭제
4. 현재 access token은 블랙리스트 등록
```

#### 구현 포인트

- `KeycloakAuthService`: token/logout/admin API 호출 전담
- `KeycloakAuthProperties`: issuer/token/logout/client 설정 전담
- `RefreshTokenRepository`: email -> refresh token 외에 `refresh token -> email` 역인덱스 추가
- `AuthResponse`: 정적 팩토리 메서드로 발급/재발급 응답 의도 분리
- `AccessTokenBlacklistFilter`: OAuth2 Resource Server 검증과 별개로 로그아웃 토큰 즉시 차단
- `UserJwtAuthenticationConfig`: Keycloak JWT를 기존 `UserPrincipal`로 변환

남아 있는 과도기:

- 로그인 시작 엔드포인트는 아직 `user-service`가 제공하지만, 실제 인증과 토큰 발급은 Keycloak이 담당한다.
- Google/Kakao provider의 실제 client id/secret 연결은 Keycloak admin console 또는 realm import 이후 운영값으로 마무리해야 한다.

---

### 10. DB 스키마 분리

MSA에서 서비스별 DB를 완전히 분리하는 것이 이상적이지만, 현재는 PostgreSQL 단일 인스턴스를 사용한다.
완전한 DB 분리 대신 스키마를 서비스별로 나눠서 테이블 격리를 적용했다.

```
PostgreSQL 1대
├── schema: user_service    → users, outbox_events, shedlock
├── schema: product_service → products, user_snapshots
├── schema: auction_service → auctions
└── schema: chat_service    → chat_rooms, chat_messages, user_snapshots
```

각 서비스의 `application.yaml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/bowchat?currentSchema=user_service
```

나중에 서비스별로 DB를 실제 분리할 때 URL만 변경하면 된다.

---

## 전체 구조 변화

```
[기존 - 모놀리식]
bowchat (단일 서비스)
└── user, chatroom, auction, product 도메인이 같은 JVM 안에서 직접 참조

[변경 후 - MSA]
user-service (port: 8081)
├── 로컬 로그인 검증 후 Keycloak token endpoint 연동
├── Keycloak 브로커 기반 OAuth2 로그인 처리
├── 사용자 로그인/리프레시 access token은 Keycloak 기준
├── 내부 API는 Keycloak Bearer Token 검증
├── 로그아웃 시 Redis 블랙리스트 등록
└── 회원가입 시 outbox 저장
         ↓ 스케줄러 (1초마다)
    Kafka user.created 발행
         ↓
chat-service, product-service, auction-service
├── user.created 수신 → UserSnapshot 로컬 저장
├── 유저 조회: Redis → UserSnapshot → user-service HTTP (Lazy 동기화)
├── 사용자 토큰 검증: Keycloak JWT 검증 + UserPrincipal 변환 + Redis 블랙리스트 체크
└── 내부 API 호출: Keycloak client_credentials → Bearer Token 전달
```

---

## 다른 서비스에서 유저 정보 사용하는 방법

다른 서비스는 `user.created` 이벤트를 수신해서 로컬에 저장해두고 조회한다.
이 패턴은 업계에서 **Event-Driven Data Replication** 또는 **Read Model**이라고 부르며, MSA에서 서비스 간 데이터 의존성을 끊는 일반적인 방법이다.

유저가 없을 때는 user-service에 HTTP로 Lazy 동기화한다. 이벤트 기반 복제만으로는 서버 최초 배포 시 기존 유저 누락, 유저 삭제/수정 이벤트 유실 시 정합성 깨짐 문제가 있어서 두 방식을 조합했다.

```java
public UserSnapshot getUser(Long userId) {
    // 1. Redis 캐시 조회
    UserSnapshot cached = redisTemplate.opsForValue().get("user:" + userId);
    if (cached != null) return cached;

    // 2. 로컬 DB(UserSnapshot) 조회
    UserSnapshot local = userSnapshotRepository.findById(userId).orElse(null);
    if (local != null) {
        redisTemplate.opsForValue().set("user:" + userId, local, Duration.ofMinutes(10));
        return local;
    }

    // 3. user-service HTTP 호출 (Lazy 동기화)
    try {
        UserSnapshot snapshot = userServiceClient.getUser(userId);
        userSnapshotRepository.save(snapshot);
        redisTemplate.opsForValue().set("user:" + userId, snapshot, Duration.ofMinutes(10));
        return snapshot;
    } catch (FeignException.NotFound e) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다.");
    } catch (FeignException e) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "일시적으로 사용할 수 없습니다.");
    }
}
```

사용자 토큰 검증은 각 서비스가 직접 처리하지만, 더 이상 shared secret에 의존하지 않는다.
현재는 Keycloak issuer 공개키로 JWT 서명을 검증하고, 토큰 claim을 기존 애플리케이션 principal로 변환한다.
user-service와 달리 다른 서비스는 user DB가 없으므로 클레임만으로 인증 객체를 만들고, Redis 블랙리스트도 함께 확인한다.

```java
// 다른 서비스의 user JWT converter
UserPrincipal principal = new UserPrincipal(
        extractUserId(jwt),
        firstText(jwt, "email", "preferred_username", "sub"),
        firstText(jwt, "nickname", "preferred_username", "email", "sub"),
        resolveRole(jwt)
);
```

반면 내부 서비스 간 호출은 더 이상 shared secret을 쓰지 않는다.
`auction-service`, `chat-service`, `product-service`는 Keycloak에서 access token을 발급받아 `user-service`의 `/internal/**`를 호출한다.

```text
auction-service/chat-service/product-service
    -> Keycloak token endpoint
    -> access token 발급(client_credentials)
    -> Authorization: Bearer <token>
    -> user-service /internal/**
    -> issuer / scope / role 검증
```

---

## 실행 시 필요한 인증 관련 환경변수

### user-service

```text
DB_USERNAME=...
DB_PASSWORD=...
REDIS_HOST=...
REDIS_PORT=...
REDIS_PASSWORD=...
OAUTH2_ISSUER_URI=http://localhost:8080/realms/bowchat
OAUTH2_INTERNAL_CLIENT_ID=user-service
KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/bowchat
KEYCLOAK_TOKEN_URI=http://localhost:8080/realms/bowchat/protocol/openid-connect/token
KEYCLOAK_LOGOUT_URI=http://localhost:8080/realms/bowchat/protocol/openid-connect/logout
KEYCLOAK_LOGIN_CLIENT_ID=bowchat-login
KEYCLOAK_LOGIN_CLIENT_SECRET=bowchat-login-secret
KEYCLOAK_ADMIN_CLIENT_ID=bowchat-admin
KEYCLOAK_ADMIN_CLIENT_SECRET=bowchat-admin-secret
KEYCLOAK_BROWSER_CLIENT_ID=bowchat-web
KEYCLOAK_BROWSER_CLIENT_SECRET=bowchat-web-secret
```

### auction-service / chat-service / product-service 공통

```text
OAUTH2_INTERNAL_REGISTRATION_ID=internal-api
OAUTH2_CLIENT_ID=<service-client-id>
OAUTH2_CLIENT_SECRET=<service-client-secret>
OAUTH2_TOKEN_URI=http://localhost:8080/realms/bowchat/protocol/openid-connect/token
OAUTH2_ISSUER_URI=http://localhost:8080/realms/bowchat
OAUTH2_SCOPES=user.internal.read
```

주의:

- `INTERNAL_SECRET`는 더 이상 사용하지 않는다.
- `JWT_SECRET`도 user-service에서는 더 이상 사용하지 않는다.
- 일반 사용자 access token을 제대로 해석하려면 Keycloak access token에 `userId`, `nickname`, `role` claim이 포함되도록 protocol mapper를 맞춰야 한다.
- 남은 과제는 Keycloak에 Google/Kakao identity provider를 실제 운영 값으로 연결하는 것이다.
- 현재 내부 API scope는 `user.internal.read`로 통일되어 있다.

---

## 인증 시퀀스 요약 (2026-06)

현재 user-service는 `인증 서버`라기보다 `Keycloak과 사용자 도메인 사이의 조정자` 역할을 맡는다.
토큰의 발급자와 검증 기준은 Keycloak이고, user-service는 회원가입/프로필/세션 보조 책임을 가진다.

### 로그인

```text
Client -> /auth/login
       -> user-service가 로컬 사용자 확인
       -> Keycloak 사용자 속성/비밀번호 동기화
       -> Keycloak token endpoint(password grant)
       -> Keycloak access token / refresh token 발급
       -> refresh token 저장
       -> access token 반환
```

### 리프레시

```text
Client -> /auth/refresh
       -> 쿠키의 refresh token 추출
       -> Redis 저장 세션과 일치 여부 확인
       -> Keycloak token endpoint(refresh_token grant)
       -> 새 access token / refresh token 발급
       -> refresh token 교체 저장
       -> 새 access token 반환
```

### 로그아웃

```text
Client -> /auth/logout
       -> user-service가 Keycloak logout 호출
       -> refresh token 삭제
       -> 현재 access token을 Redis blacklist 등록
       -> 각 서비스는 blacklist 토큰 즉시 거부
```

### 내부 API 호출

```text
auction/chat/product-service
    -> Keycloak client_credentials
    -> access token 발급
    -> Authorization: Bearer <token>
    -> user-service /internal/**
    -> issuer / scope / role 검증
```

### 현재 Keycloak 책임

- 사용자 로그인 인증
- 사용자 access token / refresh token 발급
- refresh token 재발급
- 내부 서비스 토큰 발급
- JWT 공개키/issuer 기준 제공
- role/scope 기반 인증 기준 제공

### 현재 user-service 책임

- 회원가입과 프로필 저장
- Keycloak 사용자 동기화
- refresh token 세션 연결 관리
- 로그아웃 API 제공
- access token blacklist 등록

### 아직 남은 과도기

- 애플리케이션 코드는 이미 Keycloak 브로커 기준으로 전환됐다.
- 최종적으로는 소셜 로그인도 Keycloak로 수렴하는 것이 목표다.

### Keycloak 브로커 추가 설정

Google/Kakao 소셜 로그인은 이제 애플리케이션이 직접 provider를 호출하지 않고, Keycloak이 broker 역할을 맡는다.
따라서 Keycloak admin console 또는 realm import 이후 추가 설정이 필요하다.

필수 설정:
- client: `bowchat-web`
  - redirect uri: `http://localhost:8081/login/oauth2/code/keycloak`
- identity provider alias: `google`
- identity provider alias: `kakao`
- 각 provider에서 access token에 사용자 email 제공
- `bowchat.user.claims` scope가 login client와 browser client에 포함

권장 매퍼 설정:
- client scope: `bowchat.user.claims`
- mapper 1: user attribute `userId` -> token claim `userId` (`String` 또는 `long`으로 일관되게 유지)
- mapper 2: user attribute `nickname` -> token claim `nickname`
- mapper 3: user attribute `role` -> token claim `role`
- mapper 4: 기본 email claim이 비어 있지 않도록 `email` scope와 email mapper 유지
- 위 매퍼는 최소 `access token`에는 포함, 가능하면 `userinfo`에도 같이 노출

점검 순서:
1. Keycloak 사용자 attributes에 `userId`, `nickname`, `role`가 실제로 저장되는지 확인
2. `bowchat.user.claims` client scope가 `bowchat-web`, `bowchat-login`에 연결됐는지 확인
3. Google/Kakao 로그인 후 access token payload에 `userId`, `email`, `nickname`, `role`가 모두 존재하는지 확인
4. 값이 빠지면 provider 설정이 아니라 mapper 또는 client scope 연결 문제로 먼저 본다

애플리케이션 엔드포인트:
- `/oauth2/authorization/google` -> Keycloak + `kc_idp_hint=google`
- `/oauth2/authorization/kakao` -> Keycloak + `kc_idp_hint=kakao`
- `/oauth2/authorization/keycloak` -> Keycloak 기본 로그인 화면

OAuth2 로그인 후 클라이언트 처리:
1. 사용자는 `/oauth2/authorization/google|kakao|keycloak`로 로그인 시작
2. 로그인 성공 후 user-service는 `refresh token`만 `HttpOnly` 쿠키로 저장
3. `access token`은 URL query string으로 전달하지 않는다
4. 웹/모바일 클라이언트는 리다이렉트 직후 `/auth/refresh`를 호출해서 새 access token을 받아야 한다
5. `/auth/refresh` 성공 후에는 `/auth/me`를 호출해서 현재 로그인 사용자 정보(`userId`, `email`, `nickname`, `role`)를 조회할 수 있다
6. 이후 API 호출은 응답 헤더 또는 바디로 받은 access token을 `Authorization: Bearer ...`에 담아 사용한다

이유:
- access token을 URL에 붙이면 브라우저 히스토리, 프록시 로그, referer로 유출될 수 있다.
- refresh token은 `HttpOnly` 쿠키에 두고, access token만 짧게 재발급해서 쓰는 편이 안전하다.

로컬 비밀번호 로그인 과도기 제어:
- `APP_AUTH_LOCAL_PASSWORD_LOGIN_ENABLED=true` 이면 기존 `/auth/login` 비밀번호 로그인 유지
- `APP_AUTH_LOCAL_PASSWORD_LOGIN_ENABLED=false` 이면 `/auth/login`은 차단되고 Keycloak/OAuth2 로그인만 허용
- 운영에서 최종 전환할 때 이 값을 `false`로 내리면 password grant 제거 단계로 이동하기 쉽다


