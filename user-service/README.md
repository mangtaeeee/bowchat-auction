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

    // email + provider 복합 조회
    ProviderType provider = (providerName == null)
            ? ProviderType.LOCAL
            : ProviderType.valueOf(providerName);

    User user = userRepository.findByEmailAndProvider(email, provider)
            .orElseThrow(...);
    ...
}
```

문제가 두 가지였다. 첫째, `provider` 클레임 존재 여부를 분기 처리해야 했다.
둘째, email + provider 복합 조건 조회라 PK 조회보다 느렸다.

**변경 후**
```java
public Authentication getAuthentication(String token) {
    Claims claims = parseClaims(token);
    Long userId = claims.get("userId", Long.class);

    // PK 단일 조회
    User user = userRepository.findById(userId)
            .orElseThrow(...);

    PrincipalDetails principal = new PrincipalDetails(user);
    return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
}
```

토큰 발급 시 `userId`를 클레임에 포함시켰기 때문에 가능한 변경이다.
`provider` 클레임도 더 이상 필요 없어서 토큰 구조가 단순해졌다.

화면에서 로그인할 때는 email + password를 전송하고, 발급된 토큰 안에 userId가 담긴다.
이후 모든 API 요청은 헤더에 토큰을 붙여서 보내고, `JwtAuthenticationFilter`가 토큰을 파싱해서 userId를 꺼내 DB를 조회하는 방식이다. 화면에서 userId를 직접 입력하는 게 아니다.

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

---

### 4. 회원가입 이벤트 발행 추가

MSA 환경에서 다른 서비스가 유저 정보를 필요로 할 때마다 user-service에 HTTP 요청을 보내는 방식은 여러 문제가 있다.

- user-service 장애 시 다른 서비스도 유저 정보 조회 불가
- 모든 서비스가 user-service에 결합
- 트래픽이 user-service에 집중

이를 해결하기 위해 회원가입 시 Kafka에 `user.created` 이벤트를 발행하는 방식을 선택했다.
다른 서비스는 이 이벤트를 구독해서 필요한 유저 정보를 로컬 DB에 복제(`UserSnapshot`)해두고, 이후에는 user-service를 거치지 않고 로컬 DB에서 조회한다.

이 패턴은 업계에서 **Event-Driven Data Replication** 또는 **Read Model**이라고 부르며, MSA에서 서비스 간 데이터 의존성을 끊는 일반적인 방법이다.

**기존 UserService**
```java
public void signup(SingUpRequest request) {
    String encodedPassword = passwordEncoder.encode(request.password());
    userRepository.save(User.createLocalUserFromRequest(request, encodedPassword));
    // 이벤트 발행 없음
}
```

**변경 후**
```java
public void signup(SingUpRequest request) {
    String encodedPassword = passwordEncoder.encode(request.password());
    User user = userRepository.save(User.createLocalUserFromRequest(request, encodedPassword));

    // save() 이후 - userId가 할당된 시점에 발행
    userEventPublisher.publishUserCreatedEvent(UserCreatedEvent.of(user));
}
```

이벤트에 `password`는 포함하지 않았다. Kafka 메시지는 여러 서비스가 수신하므로 민감 정보를 포함하면 안 된다.

```java
public record UserCreatedEvent(
        Long userId,
        String email,
        String nickName   // password 제외
) { ... }
```

파티션 키는 `userId`로 설정해서 동일 유저의 이벤트가 순서대로 처리되도록 했다.

```java
kafkaTemplate.send("user.created", String.valueOf(event.userId()), event);
```

---

### 5. OAuth2 회원가입에도 이벤트 발행 적용

로컬 회원가입뿐 아니라 Google, Kakao 등 SNS 로그인으로 신규 유저가 생성될 때도 동일하게 이벤트를 발행해야 한다.

**기존**
```java
private User registerNewUser(ProviderType provider, OAuth2UserInfo oAuth2UserInfo) {
    User newUser = User.builder()...build();
    return userRepository.save(newUser);
    // 이벤트 발행 없음
}
```

**변경 후**
```java
private User registerNewUser(ProviderType provider, OAuth2UserInfo oAuth2UserInfo) {
    User newUser = User.builder()...build();
    User savedUser = userRepository.save(newUser);

    userEventPublisher.publishUserCreatedEvent(UserCreatedEvent.of(savedUser));

    return savedUser;
}
```

기존 유저가 재로그인하는 경우(`updateExistingUser`)에는 이벤트를 발행하지 않는다.
이미 다른 서비스에 해당 유저의 스냅샷이 존재하기 때문이다.

---

## 전체 구조 변화

```
[기존 - 모놀리식]
bowchat (단일 서비스)
└── user, chatroom, auction, product 도메인이 같은 JVM 안에서 직접 참조

[변경 후 - MSA]
user-service (port: 8081)
├── JWT 발급 (로컬/SNS 통일)
├── OAuth2 로그인 처리
└── 회원가입 시 user.created 이벤트 발행
         ↓ Kafka
chat-service, product-service, auction-service
└── user.created 수신 → UserSnapshot 로컬 저장 → 로컬 조회
```

---

## 다른 서비스에서 유저 정보 사용하는 방법

다른 서비스는 `user.created` 이벤트를 수신해서 로컬에 저장해두고 조회한다.

```java
// 각 서비스에 UserSnapshot 엔티티 추가
@Entity
public class UserSnapshot {
    private Long userId;
    private String email;
    private String nickname;
}

// user.created 이벤트 수신
@KafkaListener(topics = "user.created")
public void handleUserCreated(UserCreatedEvent event) {
    userSnapshotRepository.save(UserSnapshot.builder()
            .userId(event.userId())
            .email(event.email())
            .nickname(event.nickName())
            .build());
}
```

토큰 검증도 각 서비스가 직접 처리한다. JWT secret key를 환경변수로 동일하게 공유하므로 user-service에 요청하지 않고 각 서비스에서 서명 검증 후 클레임에서 `userId`를 꺼내 사용한다.
user-service와 달리 다른 서비스는 user DB가 없으므로 클레임만으로 인증 객체를 만든다.

```java
// 다른 서비스의 JwtProvider - DB 조회 없이 클레임만 파싱
public Authentication getAuthentication(String token) {
    Claims claims = parseClaims(token);
    Long userId     = claims.get("userId", Long.class);
    String email    = claims.getSubject();
    String nickname = claims.get("nickname", String.class);
    String role     = claims.get("role", String.class);

    UserPrincipal principal = new UserPrincipal(userId, email, nickname, role);
    return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
}
```
