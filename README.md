# Kafka 기반 실시간 경매 플랫폼 (bowchat-auction)

![Java](https://img.shields.io/badge/Java_17-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.x-6DB33F?style=flat-square&logo=spring-boot&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=flat-square&logo=apache-kafka&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-336791?style=flat-square&logo=postgresql&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=flat-square&logo=mongodb&logoColor=white)

모놀리식으로 시작한 경매/채팅 서비스를 MSA로 분리하고, Kafka 기반 이벤트 흐름과 실시간 입찰/채팅, UserSnapshot 동기화, JWT 인증 구조를 적용한 프로젝트다.

k6로 1000명 동시 입찰 환경을 시뮬레이션했고 HTTP 실패율을 `73% -> 58% -> 0.42%`까지 낮췄다.

> 모놀리식 버전: [bowchat-monolithic](https://github.com/mangtaeeee/bowchat-monolithic)

---

## 서비스별 작업 기록

| 서비스 | 설명 | 상세 기록 |
|--------|------|-----------|
| user-service | JWT 발급, OAuth2, Outbox 패턴, 블랙리스트 | [README](./user-service/README.md) |
| product-service | 상품 등록/조회, UserSnapshot, FeignClient | [README](./product-service/README.md) |
| auction-service | 경매 시작/입찰, 낙관적 락, 입찰 브로드캐스트 | [README](./auction-service/README.md) |
| chat-service | WebSocket 채팅, Kafka Consumer, 전략 패턴 | [README](./chat-service/README.md) |

---

## 주요 성과

| 지표 | 개선 전 | 1차 개선 | 2차 개선 |
|------|---------|---------|---------|
| HTTP 실패율 | 73% | 58% | 0.42% |
| Kafka Consumer Lag | 100+ | 40 이하 | 거의 0 |
| TPS | 불안정 | 40 유지 | 안정 유지 |
| CPU 사용률 | 0.07 | 0.4+ | 0.6+ |

---

## 왜 MSA로 전환했는가

처음에는 모놀리식 구조에 Kafka를 붙였다.
입찰 이벤트를 Kafka로 발행하고 Consumer가 처리하는 구조였지만, 모든 도메인이 같은 JVM 안에 있으면 결국 Kafka가 메서드 호출을 한 번 더 감싼 수준에 가까웠다.

```text
[모놀리식 + Kafka]
같은 서비스 안에서
Producer -> Kafka -> Consumer
결국 DB도 같고 코드도 같다
```

Kafka의 핵심은 이벤트를 발행했을 때 여러 서비스가 각자 독립적으로 반응하는 데 있다.
서비스를 분리한 뒤에야 `user.created`, `auction-bid` 같은 이벤트가 실제 이벤트 버스처럼 동작하게 됐다.

```text
[MSA + Kafka]
user-service -> user.created
             -> product-service UserSnapshot 저장
             -> auction-service UserSnapshot 저장
             -> chat-service UserSnapshot 저장

auction-service -> auction-bid
                -> chat-service WebSocket 브로드캐스트
```

---

## 시스템 아키텍처

```text
Client
  |- REST / WebSocket
  |- user-service    (8081) -> JWT 발급, OAuth2, 회원가입
  |- product-service (8082) -> 상품 등록/조회
  |- auction-service (8083) -> 경매 시작/입찰
  `- chat-service    (8084) -> WebSocket 채팅, Kafka Consumer

Kafka (Event Bus)
  |- user.created -> product/auction/chat-service 수신
  |- chat-message -> chat-service 수신 (MongoDB 저장 + 브로드캐스트)
  `- auction-bid  -> chat-service 수신 (MongoDB 저장 + 브로드캐스트)

Redis (database 0 공통, prefix로 논리 분리)
  |- blacklist:{token}
  |- refresh_token:{email}
  |- product:user:{userId}
  |- auction:user:{userId}
  `- chat:user:{userId}

PostgreSQL (스키마 분리)
  |- user_service    -> users, outbox_events, shedlock
  |- product_service -> products, product_images, user_snapshots
  |- auction_service -> auctions, auction_bids, user_snapshots
  `- chat_service    -> chat_rooms, chatroom_participants, user_snapshots

MongoDB
  `- chat_messages -> 채팅 메시지 저장
```

---

## 핵심 설계 결정

### 1. Outbox 패턴으로 회원가입과 이벤트 발행 정합성 보장

회원가입 트랜잭션 안에서 Kafka를 직접 발행하면 DB 저장과 이벤트 발행의 원자성이 깨질 수 있다.
그래서 user-service는 DB 저장과 outbox 저장을 한 트랜잭션으로 묶고, 스케줄러가 outbox를 읽어 Kafka로 발행한다.

```text
DB 저장 + outbox 저장 (같은 트랜잭션)
    -> OutboxScheduler polling
    -> Kafka 발행 성공 후 markPublished
```

관련 코드:
- [OutboxEvent](./user-service/src/main/java/com/example/userservice/event/outbox/OutboxEvent.java)
- [OutboxEventPublisher](./user-service/src/main/java/com/example/userservice/event/OutboxEventPublisher.java)
- [OutboxScheduler](./user-service/src/main/java/com/example/userservice/event/outbox/scheduler/OutboxScheduler.java)

### 2. UserSnapshot + Lazy 동기화로 서비스 간 유저 정보 공유

다른 서비스가 user-service DB를 직접 참조하지 않도록 `user.created` 이벤트를 받아 로컬 `UserSnapshot`을 저장했다.
캐시와 로컬 스냅샷이 모두 없을 때만 user-service 내부 API를 호출하는 Lazy 동기화 구조를 적용했다.

```text
Redis cache -> miss
Local UserSnapshot -> miss
user-service internal API -> save locally -> return
```

관련 코드:
- [product-service UserEventConsumer](./product-service/src/main/java/com/example/productservice/user/event/UserEventConsumer.java)
- [auction-service UserSnapshotSaver](./auction-service/src/main/java/com/example/auctionservice/user/service/UserSnapshotSaver.java)
- [chat-service UserQueryService](./chat-service/src/main/java/com/example/chatservice/user/service/UserQueryService.java)

### 3. JWT에서 userId를 추출하고 요청 DTO에서는 제거

MSA로 분리한 뒤에는 클라이언트가 `userId`를 직접 보내는 구조를 줄이고, 인증된 사용자 정보는 JWT 클레임에서만 꺼내도록 정리했다.
이렇게 해야 요청 바디의 `userId` 위변조를 막을 수 있다.

관련 코드:
- [auction-service AuctionController](./auction-service/src/main/java/com/example/auctionservice/controller/AuctionController.java)
- [chat-service ChatRoomController](./chat-service/src/main/java/com/example/chatservice/chatroom/controller/ChatRoomController.java)
- [chat-service ChatRoomManager](./chat-service/src/main/java/com/example/chatservice/chatroom/service/ChatRoomManager.java)

### 4. 경매 입찰은 로컬 검증 + 낙관적 락으로 처리

입찰마다 product-service를 호출하지 않도록 경매 시작 시점에 `sellerId`를 `Auction`에 저장했다.
이후 입찰 검증은 로컬 DB에서만 수행하고, 동시 입찰 충돌은 JPA `@Version` 기반 낙관적 락으로 감지한다.

관련 코드:
- [Auction](./auction-service/src/main/java/com/example/auctionservice/entity/Auction.java)
- [AuctionBidService](./auction-service/src/main/java/com/example/auctionservice/service/AuctionBidService.java)
- [AuctionService](./auction-service/src/main/java/com/example/auctionservice/service/AuctionService.java)

### 5. WebSocket -> Kafka -> MongoDB 흐름으로 채팅/입찰 이벤트 처리

채팅과 입찰 이벤트는 chat-service가 Kafka를 구독해 MongoDB에 저장하고 WebSocket으로 브로드캐스트한다.
실시간성과 저장 책임을 chat-service로 모아 다른 서비스가 WebSocket 세부 구현을 몰라도 되게 했다.
또한 chat-service를 여러 인스턴스로 확장하면 Kafka 메시지를 소비한 인스턴스와 실제 WebSocket 세션이 붙어 있는 인스턴스가 다를 수 있으므로, Kafka는 저장 책임만 맡기고 Redis Pub/Sub로 각 chat-service 인스턴스에 이벤트를 fan-out한 뒤 각 인스턴스가 자신의 로컬 세션에 브로드캐스트하도록 분리했다.

관련 코드:
- [ChatWebSocketHandler](./chat-service/src/main/java/com/example/chatservice/websocket/ChatWebSocketHandler.java)
- [ChatKafkaConsumer](./chat-service/src/main/java/com/example/chatservice/chat/ChatKafkaConsumer.java)
- [RedisChatBroadcastPublisher](./chat-service/src/main/java/com/example/chatservice/websocket/RedisChatBroadcastPublisher.java)
- [RedisChatBroadcastSubscriber](./chat-service/src/main/java/com/example/chatservice/websocket/RedisChatBroadcastSubscriber.java)
- [ChatMessageService](./chat-service/src/main/java/com/example/chatservice/chatmessage/service/ChatMessageService.java)
- [ChatMessageRepository](./chat-service/src/main/java/com/example/chatservice/chatmessage/repository/ChatMessageRepository.java)

### 6. 내부 API는 Docker Network + X-Service-Token으로 보호

내부 API는 네트워크 레벨과 애플리케이션 레벨 두 층으로 막았다.
외부 클라이언트가 `/internal/**` 경로에 직접 접근하지 못하게 하고, 서비스 간 호출에는 별도 토큰을 요구한다.

관련 코드:
- [user-service InternalAuthInterceptor](./user-service/src/main/java/com/example/userservice/config/InternalAuthInterceptor.java)
- [product-service InternalProductController](./product-service/src/main/java/com/example/productservice/controller/InternalProductController.java)
- [auction-service FeignConfig](./auction-service/src/main/java/com/example/auctionservice/auth/config/FeignConfig.java)

---

## 장애 및 복구 시나리오

| 상황 | 영향 | 현재 대응 | 한계 |
|------|------|-----------|------|
| `user.created` 발행 실패 | 다른 서비스에 사용자 정보 전파 지연 | Outbox polling 재시도, 발행 성공 후 published 처리 | 즉시 일관성은 보장하지 않음 |
| chat-service 다운 | 실시간 채팅/입찰 브로드캐스트 중단 | Kafka에 적재된 이벤트를 chat-service 복구 후 다시 소비 | 다운 중에는 실시간 수신 불가 |
| chat-service 멀티 인스턴스 | Kafka를 소비한 인스턴스에만 브로드캐스트되면 일부 사용자 수신 누락 가능 | Redis Pub/Sub로 모든 인스턴스에 fan-out 후 각 인스턴스가 로컬 세션 브로드캐스트 | Redis 의존성 추가 |
| consumer 재처리 | 동일 이벤트가 다시 소비될 수 있음 | DLQ, 재시도, 스냅샷 저장 시 멱등성 지향 로직 사용 | exactly-once는 아님 |
| 중복 입찰/동시 입찰 | 같은 경매에 경쟁 상태 발생 | `@Version` 낙관적 락으로 충돌 감지 후 실패 처리 | 클라이언트 재시도 UX는 단순함 |
| Redis 장애 | 캐시/블랙리스트 조회 실패 가능 | 일부 조회는 로컬 DB 또는 내부 API fallback 가능 | 인증/캐시 경로 지연 증가, 일부 기능 영향 가능 |
| product-service 호출 실패 | 경매 시작 검증 실패 | Feign 예외를 `404`, `503`으로 매핑 | 상품 검증은 외부 서비스 가용성에 의존 |

---

## 성능 개선 과정

### 문제 상황

- HTTP 실패율 73%
- Kafka Consumer Lag 급증
- DB 커넥션 제한 도달

단일 Consumer가 몰리는 트래픽을 따라가지 못해 메시지 적체와 HTTP 실패가 함께 발생했다.

### 1차 개선

- Consumer 병렬 처리 적용 (`concurrency = 4`)
- DB Pool 확장 (`10 -> 30`)
- Thread Pool 확장
- Kafka Producer/Consumer 튜닝

결과:
- HTTP 실패율 `73% -> 58%`
- Kafka Lag 감소

### 2차 개선

문제:
- Partition 수에 따른 병렬 처리 상한 고정
- `max-poll-records`가 너무 커서 실패 시 재처리 비용 증가
- `fetch-min-size`가 커서 불필요한 지연 발생

개선:

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| Partition | 4 | 6 |
| concurrency | 4 | 6 |
| max-poll-records | 1000 | 500 |
| fetch-min-size | 1MB | 512KB |
| fetch-max-wait | 1000ms | 500ms |

결과:
- HTTP 실패율 `0.42%`
- Kafka Consumer Lag 거의 0
- DLQ는 예외 케이스에서만 발생

핵심은 Lag를 뒤에서 줄이는 것보다, 처음부터 쌓이지 않도록 poll/partition/concurrency를 같이 맞추는 것이었다.

---

## 현재 한계와 트레이드오프

- Saga는 적용하지 않았고, 서비스 간 분산 트랜잭션은 보장하지 않는다.
- 이벤트 처리는 exactly-once 대신 at-least-once를 전제로 두고 멱등성으로 보완하는 방향을 택했다.
- CDC 대신 polling outbox를 사용해 구현 복잡도를 낮췄다.
- `UserSnapshot`은 eventual consistency를 허용한다.
- Redis는 성능 최적화와 토큰 관리에 중요하지만, 장애 시 fallback과 운영 대응이 필요하다.
- 채팅 메시지 저장소로 MongoDB를 선택했지만, 현재 규모만 보면 PostgreSQL로도 충분히 운영 가능하다.

---

## 주요 기능

### 실시간 채팅
- WebSocket + `JwtHandshakeInterceptor` 기반 인증
- Kafka `chat-message`, `auction-bid` 소비 후 WebSocket 브로드캐스트
- MongoDB에 채팅 메시지 저장

### 경매
- 경매 시작 시 product-service를 통한 판매자 검증
- 입찰 이벤트 처리 및 실시간 가격 반영
- 낙관적 락 기반 동시 입찰 충돌 감지

### 인증
- JWT 기반 인증
- Redis RefreshToken 관리
- 로그아웃 블랙리스트를 서비스 간 공용으로 사용

---

## 모니터링

Prometheus + Grafana로 다음 지표를 확인했다.

- Kafka Consumer Lag
- HTTP Error Rate
- DB Active Connections
- JVM Memory / Thread

---

## 기술 스택

- **Backend:** Java 17, Spring Boot 3.x, Spring Security, Spring Cloud OpenFeign
- **Message Queue:** Apache Kafka
- **Database:** PostgreSQL, MongoDB
- **Cache:** Redis
- **Real-time:** WebSocket
- **Scheduling / Lock:** ShedLock
- **Monitoring:** Prometheus, Grafana
- **Load Test:** k6
- **Infra:** Docker, Docker Compose, AWS EC2

---

## 실행 방법

```bash
git clone https://github.com/mangtaeeee/bowchat-auction.git
cd bowchat-auction/infra
docker-compose -f docker-compose-local.yml up -d
```

---

## 관련 글

- [Kafka Consumer Lag 개선기](https://kimmangtae.tistory.com/35)
- [DLQ 적용 과정](https://kimmangtae.tistory.com/38)

---

## Contact

- Email: osp9658@gmail.com
- Blog: https://kimmangtae.tistory.com
- GitHub: https://github.com/mangtaeeee
