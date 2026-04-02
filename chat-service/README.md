# chat-service 분리 작업 기록

모놀리식 bowchat에서 채팅 도메인을 독립된 서비스로 분리했다.
WebSocket 실시간 통신, Kafka 이벤트 수신, 채팅방 타입별 전략 패턴, MongoDB 메시지 저장을 함께 정리했다.

---

## 배경

모놀리식에서는 채팅 메시지를 WebSocket으로 직접 브로드캐스트했다.
같은 JVM 안에서 Kafka를 써도 결국 메서드 호출과 다를 게 없었고, 서비스를 분리하니 Kafka가 진짜 이벤트 버스로 동작하게 됐다.

```
[모놀리식]
클라이언트 → WebSocket → 같은 JVM에서 바로 처리
→ Kafka를 쓰는 의미가 없음

[MSA]
클라이언트 → WebSocket → Kafka 발행
               ↓
chat-service Consumer → MongoDB 저장 + 브로드캐스트
auction-service → Kafka 발행 (입찰 이벤트)
               ↓
chat-service Consumer → MongoDB 저장 + 경매방 브로드캐스트
```

서비스가 분리되니 경매 입찰 이벤트도 Kafka를 통해 chat-service가 독립적으로 수신해서 처리하게 됐다.

---

## 변경 내용

### 1. MongoDB 선택 이유

채팅 메시지 저장소로 MongoDB를 선택했다.

**선택 이유:**
- 메시지 구조 변경 가능성 (스키마 유연성)
- 대용량 트래픽에서의 수평 확장 (샤딩) 고려

**트레이드오프 인지:**
현재 규모에서는 `PostgreSQL + roomId 인덱스`로도 충분히 처리 가능하다.
단순 `roomId` 기준 조회는 인덱스만 잘 걸면 MongoDB와 성능 차이가 없다.
MongoDB는 수십억 건 규모에서 샤딩이 필요할 때 진가를 발휘한다.

기술 선택의 핵심은 **왜 쓰는지 설명할 수 있어야** 한다는 것이다.
"채팅이니까 MongoDB" 식의 선택이 아니라, 확장 가능성을 고려한 선택임을 명확히 했다.

```java
@Document(collection = "chat_messages")
public class ChatMessage {
    @Id
    private String id;
    @Indexed  // roomId 기준 조회가 많으므로 인덱스 추가
    private Long roomId;
    ...
}
```

---

### 2. WebSocket + JWT 인증

WebSocket 핸드셰이크 시 JWT 토큰을 검증한다.
HTTP 요청과 달리 WebSocket은 연결이 지속되므로 핸드셰이크 시점에 인증을 처리한다.

```
클라이언트 → ws://localhost:8084/ws/chat/{roomId}?token=xxx
                ↓
JwtHandshakeInterceptor → 토큰 검증 → userId 세션에 저장
                ↓
ChatWebSocketHandler → roomId별 세션 Map 관리

roomSessions = {
  1L: [세션A, 세션B, 세션C],  // 1번 채팅방 접속자들
  2L: [세션D, 세션E]          // 2번 채팅방 접속자들
}
```

토큰은 쿼리 파라미터(`?token=xxx`)와 Authorization 헤더 모두 지원한다.

---

### 3. Kafka Consumer 3개 분리

역할에 따라 Consumer를 명확히 분리했다.

```
user.created  → UserSnapshot 저장 (다른 서비스와 동일한 패턴)

chat-message  → 일반 채팅 메시지 수신
                → MongoDB 저장
                → 해당 roomId 접속자들에게 WebSocket 브로드캐스트

auction-bid   → auction-service에서 입찰 시 발행
                → MongoDB 저장 (채팅 내역으로 기록)
                → 해당 경매방 접속자들에게 WebSocket 브로드캐스트
```

메시지를 WebSocket으로 직접 보내지 않고 Kafka를 거치는 이유:
나중에 서버가 여러 대로 늘어날 때 Kafka가 브로드캐스트를 중재해줘서 스케일아웃이 용이해진다.

---

### 4. ChatRoomManager 전략 패턴

채팅방 타입마다 입장 로직이 달라서 전략 패턴을 적용했다.

```java
public interface ChatRoomManager<T extends ChatRoomEnterRequest> {
    ChatRoomType supportType();
    Class<T> requestType();
    EnterChatResponse enterChatRoom(T request);

    // 타입 캐스팅을 인터페이스가 책임짐
    // 새 Manager 추가 시 이 메서드 오버라이드 불필요
    default EnterChatResponse enter(ChatRoomEnterRequest request) {
        return enterChatRoom(requestType().cast(request));
    }
}
```

`ChatRoomService`는 생성자 주입으로 모든 Manager를 자동 등록한다:

```java
public ChatRoomService(List<ChatRoomManager<? extends ChatRoomEnterRequest>> managers, ...) {
    this.managers = managers.stream()
            .collect(Collectors.toMap(ChatRoomManager::supportType, Function.identity()));
}
```

**확장 시 변경 불필요:** 새 채팅방 타입 추가 시 `Manager` 클래스 하나만 추가하면 `ChatRoomService` 수정 없이 동작한다. OCP(개방-폐쇄 원칙) 준수.

**타입별 입장 로직:**

| 타입 | 검증 | 채팅방 생성 조건 |
|------|------|-----------------|
| AUCTION | auction-service 경매 존재 + 종료 여부 확인 | productId 기준 단일 방 (없으면 생성) |
| DIRECT | product-service 상품 존재 + 판매자 확인 | productId + buyerId 기준 (없으면 생성) |
| GROUP | 없음 | 요청마다 새 방 생성 |

채팅방이 없으면 자동 생성하는 방식을 선택했다. 판매자가 먼저 방을 만드는 방식은 auction-service가 chat-service를 호출해야 해서 서비스 간 결합도가 올라가기 때문이다.

---

### 5. UserSnapshot 동기화

product/auction-service와 동일한 3단계 조회 전략을 적용했다.

```
Redis 캐시 (TTL 10분, prefix: chat:user:{userId})
    ↓ miss
로컬 UserSnapshot DB
    ↓ miss
user-service HTTP 호출 (FeignClient + X-Service-Token)
    ↓ 로컬 저장 후 반환
```

Redis 역직렬화 시 `Jackson2JsonRedisSerializer`가 `LinkedHashMap`으로 반환하는 문제를 `ObjectMapper.convertValue()`로 해결했다:

```java
Object cached = redisTemplate.opsForValue().get(cacheKey);
if (cached != null) {
    return objectMapper.convertValue(cached, UserSnapshot.class);
}
```

---

### 6. LazyInitializationException 해결

채팅방 조회 시 `participants`를 Lazy 로딩으로 설정했는데, 트랜잭션 밖에서 접근하면 예외가 발생했다.

```
// 문제
findByTypeAndProduct() → ChatRoom 조회
트랜잭션 종료
addOrActivateMember() → participants 접근 → LazyInitializationException
```

`ChatRoomRepository`에 `JOIN FETCH`로 participants를 함께 조회하는 쿼리를 추가해서 해결했다:

```java
@Query("SELECT c FROM ChatRoom c LEFT JOIN FETCH c.participants " +
       "WHERE c.type = :type AND c.product = :productId")
Optional<ChatRoom> findByTypeAndProductWithParticipants(
        @Param("type") ChatRoomType type,
        @Param("productId") Long productId);
```

---

## 전체 구조

```
chat-service (port: 8084)
├── JWT 클레임 파싱 + Redis 블랙리스트 체크
├── WebSocket: JwtHandshakeInterceptor → ChatWebSocketHandler
├── Kafka Consumer:
│   ├── user.created → UserSnapshot 저장
│   ├── chat-message → MongoDB 저장 + 브로드캐스트
│   └── auction-bid  → MongoDB 저장 + 브로드캐스트
├── 채팅방 입장: ChatRoomManager 전략 패턴
│   ├── AuctionChatRoomManager → auction-service FeignClient
│   ├── ProductChatRoomManager → product-service FeignClient
│   └── GroupChatRoomManager
└── 메시지 조회: ChatMessageRepository (MongoDB)

채팅 흐름:
클라이언트 WebSocket → Kafka chat-message 발행
                      → Consumer: MongoDB 저장 + 브로드캐스트

경매 입찰 흐름:
auction-service → Kafka auction-bid 발행
                → Consumer: MongoDB 저장 + 경매방 브로드캐스트
```
