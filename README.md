# Kafka 기반 실시간 경매 플랫폼 (bowchat-auction)

![Java](https://img.shields.io/badge/Java_17-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.x-6DB33F?style=flat-square&logo=spring-boot&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=flat-square&logo=apache-kafka&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-336791?style=flat-square&logo=postgresql&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=flat-square&logo=mongodb&logoColor=white)

모놀리식으로 시작해 MSA로 전환하며 서비스 간 데이터 정합성, 실시간 경매/채팅, 부하 테스트 기반 성능 개선을 구현한 프로젝트입니다.

k6로 1000명 동시 입찰 환경을 시뮬레이션했고, HTTP 실패율을 73% → 58% → 0.42%까지 낮췄습니다.

---

## 서비스별 작업 기록

| 서비스 | 설명 | 상세 기록 |
|--------|------|-----------|
| user-service | JWT 발급, OAuth2, 아웃박스 패턴, 블랙리스트 | [README](./user-service/README.md) |
| product-service | 상품 등록/조회, UserSnapshot, FeignClient | [README](./product-service/README.md) |
| auction-service | 경매 시작/입찰, 낙관적 락, 브로드캐스트 | [README](./auction-service/README.md) |
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

처음에는 모놀리식 구조에 Kafka를 붙였습니다.
입찰 이벤트를 Kafka로 발행하고 Consumer가 처리하는 구조였지만, 모든 도메인이 같은 JVM 안에 있으니 결국 Kafka를 쓰는 의미가 반감됐습니다.

```
[모놀리식 + Kafka]
같은 서비스 안에서
Producer → Kafka → Consumer
결국 DB도 같고, 코드도 같고
→ 그냥 메서드 호출과 다를 게 없는 구조
```

Kafka의 핵심은 **이벤트를 발행하면 구독한 서비스들이 각자 독립적으로 반응**하는 것입니다.
모놀리식에서는 이 원리를 살릴 수 없었습니다.

```
[MSA + Kafka]
user-service   → user.created 발행
                    ↓
product-service → UserSnapshot 저장 (독립적)
auction-service → UserSnapshot 저장 (독립적)
chat-service    → UserSnapshot 저장 (독립적)

auction-service → auction-bid 발행
                    ↓
chat-service    → WebSocket 브로드캐스트 (독립적)
```

서비스가 분리되니 Kafka가 진짜 이벤트 버스로 동작하게 됐습니다.
각 서비스는 이벤트만 구독하고, 발행한 서비스가 누군지 알 필요가 없습니다.

---

## 시스템 아키텍처

```
Client
  ↓ REST / WebSocket
  ├── user-service    (8081) → JWT 발급, OAuth2, 회원가입
  ├── product-service (8082) → 상품 등록/조회
  ├── auction-service (8083) → 경매 시작/입찰
  └── chat-service    (8084) → WebSocket 채팅, Kafka Consumer

Kafka (Event Bus)
  ├── user.created   → product/auction/chat-service 수신
  ├── chat-message   → chat-service 수신 (MongoDB 저장 + 브로드캐스트)
  └── auction-bid    → chat-service 수신 (MongoDB 저장 + 브로드캐스트)

Redis (database: 0 공통, prefix로 논리적 분리)
  ├── blacklist:{token}         → 로그아웃 블랙리스트 (전 서비스 공유)
  ├── refresh_token:{email}     → user-service
  ├── product:user:{userId}     → product-service UserSnapshot 캐시
  ├── auction:user:{userId}     → auction-service UserSnapshot 캐시
  └── chat:user:{userId}        → chat-service UserSnapshot 캐시

PostgreSQL (스키마 분리)
  ├── user_service    → users, outbox_events, shedlock
  ├── product_service → products, product_images, user_snapshots
  ├── auction_service → auctions, auction_bids, user_snapshots
  └── chat_service    → chat_rooms, chatroom_participants, user_snapshots

MongoDB
  └── chat_messages   → 채팅 메시지 (roomId 인덱스)
```

---

## MSA 전환 핵심 설계 결정

### 1. 아웃박스 패턴 - 이벤트 발행 정합성 보장

회원가입 트랜잭션 안에서 Kafka를 직접 발행하면 DB 저장과 이벤트 발행의 원자성이 보장되지 않습니다.

```
[문제]
DB 저장 (트랜잭션) → Kafka 발행 (트랜잭션 밖)
→ DB는 성공했는데 Kafka 실패 → 다른 서비스가 해당 유저를 영원히 모름
→ Kafka는 됐는데 DB 롤백 → 존재하지 않는 유저 이벤트 발행

[해결]
DB 저장 + outbox 저장 (같은 트랜잭션)
    ↓ 스케줄러 (1초마다, ShedLock으로 중복 실행 방지)
Kafka 발행 (sendSync - 동기 발행으로 성공 확인 후 markPublished)
```

→ [user-service 상세 기록](./user-service/README.md)

---

### 2. UserSnapshot + Lazy 동기화 - 서비스 간 유저 정보 공유

다른 서비스는 user-service DB를 직접 참조할 수 없습니다.
`user.created` 이벤트를 수신해서 로컬에 저장하고, 없으면 HTTP로 Lazy 동기화합니다.

```
Redis 캐시 (TTL 10분)
    ↓ miss
로컬 UserSnapshot (DB)
    ↓ miss
user-service HTTP 호출 (FeignClient + X-Service-Token)
    ↓ 로컬 저장 후 반환
```

이벤트 기반만으로는 서버 최초 배포 시 기존 유저 누락, 삭제/수정 이벤트 유실 시 정합성 문제가 있어 Lazy 동기화로 보완했습니다.

→ [product-service 상세 기록](./product-service/README.md)

---

### 3. 내부 API 보안 - 2레이어 보호

```
레이어 1: Docker Network 격리 (bowchat-internal)
    → 외부 클라이언트가 /internal/** 직접 접근 불가

레이어 2: X-Service-Token 헤더 검증
    → 인증된 서비스만 내부 API 호출 가능
```

---

### 4. JWT 블랙리스트 - 토큰 탈취 대응

JWT는 stateless라 만료 전 강제 무효화가 불가능합니다.
로그아웃 시 Redis에 블랙리스트를 등록하고, 각 서비스 JwtAuthenticationFilter에서 체크합니다.

```
로그아웃 → Redis blacklist:{token} 등록 (TTL = 남은 만료시간)
요청 시  → blacklist 체크 → 있으면 401
```

블랙리스트는 전 서비스가 같은 Redis database(0)를 공유합니다.
Redis Cluster 환경에서는 database 분리가 불가능하므로 키 prefix 방식으로 논리적 분리를 적용했습니다.

---

### 5. 경매 입찰 검증 - 불필요한 HTTP 호출 최소화

```
경매 시작: product-service HTTP 호출 → 상품 존재 + 판매자 확인 → sellerId Auction에 저장
입찰 시  : Auction.sellerId로 로컬 검증 (HTTP 호출 없음)
```

입찰할 때마다 product-service를 호출하면 부하가 생기므로, 경매 시작 시 한 번만 검증하고 이후에는 로컬 DB만 참조합니다.

→ [auction-service 상세 기록](./auction-service/README.md)

---

### 6. ChatRoomManager 전략 패턴 - 채팅방 타입별 확장

채팅방 타입마다 입장 로직이 달라서 전략 패턴을 적용했습니다.
새 타입 추가 시 Manager 클래스만 추가하면 ChatRoomService 수정 없이 동작합니다.

```
AUCTION → auction-service 경매 존재 확인 → 단일 경매방
DIRECT  → product-service 상품 확인 → 구매자+판매자 1:1 방
GROUP   → 그냥 생성
```

→ [chat-service 상세 기록](./chat-service/README.md)

---

### 7. MongoDB 채팅 메시지 저장

채팅 메시지 저장소로 MongoDB를 선택했습니다.
스키마 유연성과 대용량 트래픽에서의 수평 확장(샤딩)을 고려한 선택입니다.

현재 규모에서는 PostgreSQL + roomId 인덱스로도 충분히 처리 가능하나,
수십억 건 규모에서 샤딩이 필요할 때 MongoDB가 유리합니다.

→ [chat-service 상세 기록](./chat-service/README.md)

---

## 성능 개선 과정

### 문제 상황

- HTTP 실패율 73%
- Kafka Consumer Lag 급증
- DB 커넥션 풀 한계 도달

단일 Consumer와 제한된 자원으로 인해 요청이 몰리는 순간 처리 속도가 따라가지 못하는 구조였습니다.

---

### 1차 개선

**원인:** 단일 Consumer로 병렬 처리 불가, DB Pool 부족

```java
@KafkaListener(
    topics = "auction-bid",
    concurrency = "4"
)
```

- Consumer 병렬 처리 적용 (concurrency = 4)
- DB Pool 확장 (10 → 30)
- Thread Pool 확장
- Kafka Producer/Consumer 튜닝

**결과:** HTTP 실패율 73% → 58%, Kafka Lag 감소

다만 트래픽이 몰리는 구간에서는 여전히 밀림이 발생했습니다.

---

### 2차 개선

1차 개선 이후에도 Lag가 순간적으로 증가하고 DLQ가 반복적으로 쌓이는 문제가 있었습니다.

**원인 분석:**
- Partition 수에 따른 병렬 처리 상한 고정 (Partition: 4 / concurrency: 4)
- max-poll-records: 1000 → 실패 시 전체 재처리
- fetch-min-size: 1MB → 메시지 대기 발생
- 실제 로그에서 retry → rollback → DLQ 흐름이 반복됨

**개선:**

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| Partition | 4 | 6 |
| concurrency | 4 | 6 |
| max-poll-records | 1000 | 500 |
| fetch-min-size | 1MB | 512KB |
| fetch-max-wait | 1000ms | 500ms |

EC2 2코어 환경에서 I/O 바운드 특성을 고려해 CPU × 1.5 기준으로 concurrency를 설정했습니다. 무작정 늘리지 않고 안정적으로 처리 가능한 수준으로 조정했습니다.

**결과:** HTTP 실패율 0.42%, Kafka Lag 거의 0, DLQ 간헐적 발생

Lag를 줄이기보다 처음부터 쌓이지 않도록 구조를 바꾼 것이 핵심이었습니다.

---

## 주요 기능

### 실시간 채팅
- WebSocket + JwtHandshakeInterceptor JWT 인증
- Kafka chat-message 토픽으로 MongoDB 저장 + 브로드캐스트
- 경매 입찰 이벤트 실시간 브로드캐스트 (auction-bid 토픽)

### 경매
- 입찰 이벤트 처리 및 실시간 가격 반영
- 낙관적 락(@Version)으로 동시 입찰 처리
- AuctionBidService 분리로 트랜잭션/오케스트레이션 경계 명확화

### 인증
- JWT 기반 인증 (로컬/OAuth2 통일 클레임 구조)
- Redis RefreshToken 관리
- 로그아웃 블랙리스트 (전 서비스 공유)

---

## 기술적 의사결정

### Kafka 사용 이유
서비스 간 이벤트 기반 통신을 위해 도입했습니다. 단순 비동기 처리뿐 아니라 Consumer 분리를 통한 역할 기반 처리, DLQ를 활용한 실패 메시지 재처리 구조가 핵심이었습니다.

### Redis 사용 이유
RefreshToken, JWT 블랙리스트, UserSnapshot 캐시 세 가지 용도로 사용합니다. 각 서비스가 같은 Redis database(0)를 공유하되 키 prefix로 논리적 분리합니다.

### MongoDB 사용 이유
채팅 메시지 저장소로 스키마 유연성과 수평 확장 가능성을 고려해 선택했습니다.
현재 규모에서는 PostgreSQL로도 충분하나, 대용량 샤딩 시 MongoDB가 유리합니다.

### 동시성 제어
- Kafka Partition Key (경매 ID 기반)로 같은 경매 입찰의 순서 보장
- JPA @Version 낙관적 락으로 동시 입찰 충돌 감지

### DLQ
실패 메시지를 별도 토픽에 적재하고 Grafana로 모니터링, 관리자 검토 후 재처리합니다.

---

## 모니터링

Prometheus + Grafana로 다음 지표를 확인했습니다.

- Kafka Consumer Lag
- HTTP Error Rate
- DB Active Connections
- JVM Memory / Thread

---

## 기술 스택

- **Backend:** Java 17, Spring Boot 3.x, Spring Security, Spring Cloud OpenFeign
- **Message Queue:** Apache Kafka (DLQ, ShedLock)
- **Database:** PostgreSQL (스키마 분리), MongoDB
- **Cache:** Redis
- **Real-time:** WebSocket
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
- [DLQ 도입 과정](https://kimmangtae.tistory.com/38)

---

## Contact

- Email: osp9658@gmail.com
- Blog: https://kimmangtae.tistory.com
- GitHub: https://github.com/mangtaeeee