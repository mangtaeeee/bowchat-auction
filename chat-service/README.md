# chat-service 분리 작업 기록

모놀리식 bowchat에서 채팅 도메인을 독립된 서비스로 분리했다.
WebSocket 실시간 통신, Kafka 이벤트 수신, 채팅방 타입별 전략 패턴, MongoDB 메시지 저장 구조를 함께 정리했다.

---

## 배경

모놀리식에서는 채팅 메시지를 WebSocket으로 직접 브로드캐스트했다.
같은 JVM 안에서는 Kafka를 둬도 결국 메서드 호출과 크게 다르지 않았고, 서비스를 분리한 뒤에야 Kafka가 실제 이벤트 버스로 동작하게 됐다.

```text
[모놀리식]
클라이언트 -> WebSocket -> 같은 JVM 안에서 바로 처리

[MSA]
클라이언트 -> WebSocket -> Kafka 발행
                         -> chat-service consumer -> MongoDB 저장 + 브로드캐스트
auction-service -> Kafka 발행(입찰 이벤트)
                -> chat-service consumer -> MongoDB 저장 + 경매방 브로드캐스트
```

서비스가 분리되면서 경매 입찰 이벤트도 Kafka를 통해 chat-service가 독립적으로 수신하고 처리할 수 있게 됐다.

---

## 변경 내용

### 1. MongoDB 선택 이유

채팅 메시지 저장소로 MongoDB를 선택했다.

선택 이유:
- 메시지 구조 변화 가능성에 대한 유연성
- 대용량 트래픽에서의 수평 확장 가능성

트레이드오프도 분명하다.
현재 규모에서는 `PostgreSQL + roomId 인덱스`로도 충분히 처리 가능하다.
지금 MongoDB를 선택한 이유는 현재 성능보다, 메시지 저장소를 채팅 도메인에 맞게 분리하고 확장 여지를 확보하기 위해서다.

```java
@Document(collection = "chat_messages")
public class ChatMessage {
    @Id
    private String id;

    @Indexed
    private Long roomId;
    ...
}
```

---

### 2. WebSocket + JWT 인증

WebSocket은 HTTP와 달리 연결이 유지되므로, 핸드셰이크 시점에 JWT를 검증하도록 했다.
토큰이 유효하고, 해당 사용자가 실제 채팅방 활성 참여자인 경우에만 연결을 허용한다.

```text
클라이언트 -> ws://localhost:8084/ws/chat/{roomId}?token=xxx
           -> JwtHandshakeInterceptor
           -> JWT 검증 + 블랙리스트 확인 + 채팅방 참여 권한 확인
           -> ChatWebSocketHandler 연결
```

이렇게 해서 다음 우회 경로를 막았다.
- 채팅방 참여 API 없이 roomId만으로 직접 소켓 연결
- leave 이후 열린 소켓으로 계속 송수신
- 로그아웃한 토큰으로 WebSocket 재연결

토큰은 쿼리 파라미터(`?token=...`)와 `Authorization` 헤더 둘 다 지원한다.

---

### 3. Kafka Consumer 3개 분리

역할에 따라 Consumer를 명확히 분리했다.

```text
user.created  -> UserSnapshot 저장

chat-message  -> 일반 채팅 메시지 수신
              -> MongoDB 저장
              -> 채팅방 WebSocket 브로드캐스트

auction-bid   -> auction-service 입찰 이벤트 수신
              -> MongoDB 저장
              -> 경매방 WebSocket 브로드캐스트
```

메시지를 WebSocket으로 직접 보내지 않고 Kafka를 거치는 이유는, 저장과 브로드캐스트를 비동기 이벤트 흐름으로 분리해 결합도를 낮추고 서비스 확장을 쉽게 만들기 위해서다.

---

### 4. 멀티 인스턴스 환경에서 WebSocket 브로드캐스트 정합성 보장

초기 구조에서는 Kafka 메시지를 소비한 chat-service 인스턴스가 자신의 메모리에 있는 WebSocket 세션에만 브로드캐스트했다.
이 구조는 단일 인스턴스에서는 문제가 없지만, chat-service를 여러 대로 확장하면 메시지를 소비한 인스턴스와 실제 사용자가 연결된 인스턴스가 달라져 일부 사용자가 메시지를 받지 못할 수 있다.

이 문제를 해결하기 위해 역할을 분리했다.

- Kafka Consumer: 메시지 저장 책임
- Redis Pub/Sub: 인스턴스 간 이벤트 fan-out
- WebSocket Handler: 각 인스턴스의 로컬 세션 브로드캐스트

즉, Kafka로 받은 메시지를 MongoDB에 저장한 뒤 Redis Pub/Sub 채널로 다시 발행하고, 모든 chat-service 인스턴스가 이를 구독해 자신의 로컬 세션에 브로드캐스트하도록 변경했다.
이 구조로 멀티 인스턴스 환경에서도 같은 채팅방 사용자가 어느 인스턴스에 연결되어 있든 동일한 메시지를 받을 수 있게 했다.

```text
WebSocket -> Kafka -> chat-service consumer
          -> MongoDB 저장
          -> Redis Pub/Sub fan-out
          -> 각 chat-service 인스턴스가 로컬 세션 브로드캐스트
```

관련 코드:
- [ChatKafkaConsumer](./src/main/java/com/example/chatservice/chat/ChatKafkaConsumer.java)
- [RedisChatBroadcastPublisher](./src/main/java/com/example/chatservice/websocket/RedisChatBroadcastPublisher.java)
- [RedisChatBroadcastSubscriber](./src/main/java/com/example/chatservice/websocket/RedisChatBroadcastSubscriber.java)
- [ChatWebSocketHandler](./src/main/java/com/example/chatservice/websocket/ChatWebSocketHandler.java)

---

### 5. ChatRoomManager 전략 패턴

채팅방 타입마다 입장 로직이 달라서 전략 패턴을 적용했다.

```java
public interface ChatRoomManager<T extends ChatRoomEnterRequest> {
    ChatRoomType supportType();
    Class<T> requestType();
    EnterChatResponse enterChatRoom(T request, Long userId);

    default EnterChatResponse enter(ChatRoomEnterRequest request, Long userId) {
        return enterChatRoom(requestType().cast(request), userId);
    }
}
```

`ChatRoomService`는 생성자 주입으로 모든 Manager를 자동 등록한다.

```java
public ChatRoomService(List<ChatRoomManager<? extends ChatRoomEnterRequest>> managers, ...) {
    this.managers = managers.stream()
            .collect(Collectors.toMap(ChatRoomManager::supportType, Function.identity()));
}
```

이 구조의 장점은 새 채팅방 타입을 추가할 때 `ChatRoomService`를 수정하지 않고 Manager 구현체만 추가하면 된다는 점이다.

채팅방 타입별 입장 로직:

| 타입 | 검증 | 생성 기준 |
|------|------|-----------|
| AUCTION | auction-service로 경매 존재/종료 여부 확인 | productId 기준 단일 방 |
| DIRECT | product-service로 상품 존재/판매자 확인 | productId + buyerId 기준 |
| GROUP | 별도 외부 검증 없음 | 요청 시 생성 |

---

### 6. UserSnapshot 동기화

product-service, auction-service와 동일한 3단계 조회 전략을 적용했다.

```text
Redis cache (chat:user:{userId})
    -> miss
Local UserSnapshot DB
    -> miss
user-service internal API
    -> local save + return
```

Redis 역직렬화 시 `LinkedHashMap`으로 반환되는 문제는 `ObjectMapper.convertValue()`로 해결했다.

```java
Object cached = redisTemplate.opsForValue().get(cacheKey);
if (cached != null) {
    return objectMapper.convertValue(cached, UserSnapshot.class);
}
```

---

### 7. LazyInitializationException 해결

채팅방 조회 시 `participants`를 Lazy 로딩으로 두고 트랜잭션 밖에서 접근하면 예외가 발생할 수 있다.
이를 방지하기 위해 `ChatRoomRepository`에 `JOIN FETCH` 쿼리를 추가해 참여자 컬렉션을 함께 조회하도록 했다.

```java
@Query("SELECT c FROM ChatRoom c LEFT JOIN FETCH c.participants " +
       "WHERE c.type = :type AND c.product = :productId")
Optional<ChatRoom> findByTypeAndProductWithParticipants(
        @Param("type") ChatRoomType type,
        @Param("productId") Long productId);
```

---

## 전체 구조

```text
chat-service (port: 8084)
|- JWT 클레임 파싱 + Redis 블랙리스트 체크
|- WebSocket: JwtHandshakeInterceptor -> ChatWebSocketHandler
|- Kafka Consumer
|  |- user.created -> UserSnapshot 저장
|  |- chat-message -> MongoDB 저장 + Redis fan-out
|  `- auction-bid  -> MongoDB 저장 + Redis fan-out
|- Redis Pub/Sub subscriber -> 로컬 WebSocket 세션 브로드캐스트
|- 채팅방 입장: ChatRoomManager 전략 패턴
|  |- AuctionChatRoomManager -> auction-service FeignClient
|  |- ProductChatRoomManager -> product-service FeignClient
|  `- GroupChatRoomManager
`- 메시지 조회: ChatMessageRepository (MongoDB)

채팅 흐름:
클라이언트 -> WebSocket -> Kafka chat-message 발행
                        -> Consumer: MongoDB 저장
                        -> Redis Pub/Sub fan-out
                        -> 각 인스턴스 로컬 세션 브로드캐스트

경매 입찰 흐름:
auction-service -> Kafka auction-bid 발행
                -> Consumer: MongoDB 저장
                -> Redis Pub/Sub fan-out
                -> 각 인스턴스 로컬 세션 브로드캐스트
```
