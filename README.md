# BowChat - 실시간 채팅 및 경매 플랫폼

![Java](https://img.shields.io/badge/Java-17-007396?logo=java&logoColor=white),![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot),![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white),![MongoDB](https://img.shields.io/badge/MongoDB-7.0-47A248?logo=mongodb),![Kafka](https://img.shields.io/badge/Kafka-3.x-231F20?logo=apachekafka),![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?logo=redis),![Docker](https://img.shields.io/badge/Docker-20.10-2496ED?logo=docker)  

---

## 프로젝트 개요
Spring Boot, Kafka, PostgreSQL, MongoDB, Redis, WebSocket 기반의 **실시간 채팅 및 경매 서비스**입니다.  
SNS(OAuth2) 로그인과 JWT 인증을 지원하며, Kafka Consumer를 역할별로 분리해 안정적인 병렬 처리를 구현했습니다.  

---

## 시스템 아키텍처
```
Client → WebSocket → Kafka Producer
                  ↳ chat-message → SaveConsumer, BroadcastConsumer
                  ↳ chat-event   → EventConsumer
                  ↳ auction-bid  → BidConsumer

Redis → 세션 및 토큰 캐싱
MongoDB → 채팅 로그 저장
PostgreSQL → 사용자, 상품, 메타데이터 관리
```

---

## 주요 기능
- **실시간 채팅**: WebSocket + Kafka Pub/Sub
- **경매 서비스**: 입찰/낙찰 이벤트 스트림 처리 및 실시간 동기화
- **인증/보안**: OAuth2 소셜 로그인, JWT 기반 인증, Redis RefreshToken 관리
- **데이터 저장소**:  
  - PostgreSQL: 사용자 및 서비스 메타데이터  
  - MongoDB: 채팅 로그/이력  
  - Redis: 세션/토큰 캐시
- **확장성**: Kafka Consumer 역할 분리(저장, 브로드캐스트, 이벤트, 경매) 및 병렬 처리 구조

---

## 기술 스택
| 구분       | 기술 |
|------------|------|
| Language   | Java 17 |
| Framework  | Spring Boot 3.x, Spring Security |
| Database   | PostgreSQL, MongoDB |
| Cache      | Redis |
| Messaging  | Apache Kafka |
| DevOps     | Docker, Docker Compose |
| ORM/Query  | JPA, QueryDSL |

---

## Kafka 토픽 설계
| Topic 이름     | 메시지 타입           | Consumer                          |
|----------------|-----------------------|-----------------------------------|
| `chat-message` | 채팅, 파일 메시지     | SaveConsumer, BroadcastConsumer    |
| `chat-event`   | 입장, 퇴장, 시스템 이벤트 | EventConsumer                   |
| `auction-bid`  | 입찰, 낙찰 이벤트     | BidConsumer                       |

---

## Why? 기술 선택 배경

### 왜 Redis를 사용했는가?
- **세션 관리와 인증 토큰 캐싱**  
  실시간 연결(WebSocket) 환경에서는 사용자 세션이 빈번히 생성·소멸되므로,  
  DB 접근보다 훨씬 빠른 In-memory 캐시가 필요했습니다.  
  Redis를 세션 스토리지 및 RefreshToken 저장소로 사용해  
  로그인 유지와 토큰 재발급의 응답 지연을 최소화했습니다.  

- **Kafka와의 역할 분리 (Session vs Stream)**  
  Redis는 인증·세션 캐시와 같은 단기 데이터 관리에 집중하고,  
  모든 실시간 메시징과 이벤트 스트리밍은 Kafka 단일 구조로 처리했습니다.  
  이를 통해 메시지 유실 없이, 서버 수에 관계없이 안정적인 확장성과 정합성을 확보했습니다.

---

### 왜 STOMP에서 Kafka 단일 구조로 전환했는가?
- **STOMP의 한계 (단일 서버 기반, 스케일아웃 비효율)**  
  STOMP 기반 구조에서는 메시지 브로커가 서버 인스턴스 내부에 존재해  
  서버 수가 늘어날수록 세션 동기화와 브로드캐스트 관리가 복잡해졌습니다.  
  특히 경매 입찰처럼 “다수 사용자가 동일 객체를 동시에 수정하는”  
  케이스에서는 메시지 순서 보장과 재처리가 어렵다는 문제가 있었습니다.  
- **Kafka 도입 후 장점**  
  - 메시지 브로커를 외부로 분리해 서버 수와 관계없이 스케일링 가능  
  - Topic 단위로 Consumer를 분리하여 저장, 브로드캐스트, 이벤트, 입찰 로직 병렬 처리  
  - 메시지 손실 방지를 위한 **Offset Commit 구조** 및 **Replay 가능성** 확보  
  - 실시간 채팅 + 경매 로직을 동일 스트리밍 파이프라인에서 관리 가능  
  결과적으로 STOMP보다 유지보수성과 트래픽 안정성이 월등히 향상되었습니다.

---

### 왜 Kafka와 Optimistic Lock을 함께 사용했는가?
- **Kafka 파티션 기반 순차 처리 (1차 동시성 제어)**
auctionId를 파티션 키로 지정해 동일 경매의 입찰 이벤트가 항상 같은 파티션에서 순서대로 처리되도록 설계했습니다.
→ **경매 단위로 논리적 락 효과 확보**, Race Condition 방지

- **JPA 낙관적 락(@Version) 기반 정합성 보장 (2차 데이터 보호)**
Kafka가 순서를 보장하더라도, Consumer 재시작·중복 메시지 등으로 인한 커밋 충돌에 대비해
Auction 엔티티에 @Version을 적용했습니다.
→ **DB 커밋 단계에서 충돌 감지 및 자동 재시도**
→ 트랜잭션 충돌 시 마지막 입찰만 반영되어 데이터 정합성 유지

---

## Kafka 실패 처리 (DLQ 복구 구조)
- **Kafka DefaultErrorHandler + DeadLetterPublishingRecoverer**를 적용해
메시지 처리 중 예외가 발생하면 **최대 2회 재시도 후 DLQ(Dead Letter Queue)** 로 전송되도록 구성했습니다.
- .DLT 토픽으로 전송된 메시지는 이후 별도의 모니터링/재처리 컨슈머가 분석 및 복구 처리합니다.
이를 통해 Kafka Consumer 장애나 일시적 DB 예외 상황에서도
메시지 유실 없이 복구 가능한 안정적 스트리밍 구조를 확보했습니다.
예시 로그

```java
WARN  KafkaConsumerConfig : DLQ 전송: topic=auction-bid.DLT, reason=OptimisticLockingFailureException
INFO  KafkaConsumerConfig : Kafka 재시도 1회 실패: key=AUCTION:1
```
---

## 테스트 코드 자동화 (Copilot Integration)

프로젝트의 테스트 품질 및 생산성 향상을 위해 **GitHub Copilot Custom Instructions** 기반의  
**테스트 코드 자동화 환경**을 구축했습니다.

- `copilot-instructions.md`를 작성하여 **한글 주석(`// 테스트:`)** 기반으로  
  JUnit + Mockito 테스트 메서드를 자동 완성할 수 있는 구조를 설계했습니다.
- 반복적인 단위 테스트 작성 속도를 단축하고,
  서비스 로직의 테스트 커버리지와 품질 일관성을 향상시켰습니다.
- `feat: copilot 을 통한 테스트 메서드 자동화 프롬프트 추가` 커밋으로 관리 중.

**예시**
```java
// 테스트: 회원가입 성공 시 userRepository.save() 호출된다
@Test
void 회원가입_성공시_저장된다() {
    // 주어진 상황
    // 실행
    // 결과 확인
}

```

---

### 기술 구조 의도 요약
| 구분 | 기존 | 개선 후 | 개선 효과 |
|------|------|----------|------------|
| 실시간 메시징 | STOMP 기반 단일 서버 | Kafka 기반 스트리밍 구조 | 확장성 및 메시지 정합성 확보 |
| 세션 관리 | DB 기반 세션 | Redis 세션 캐시 | 응답속도 향상 및 세션 안정성 확보 |
| 입찰 동시성 | 단순 Thread Lock | Optimistic Lock + Kafka Queue | 경합 상황에서도 정합성 유지 |

---
## Kafka 토픽 생성 명령어
```bash
# chat-message 토픽
kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --topic chat-message

# chat-event 토픽
kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --topic chat-event

# auction-bid 토픽
kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --topic auction-bid
```

---

## 배포 및 인프라 구성

- AWS EC2 (t3.small) 환경에 Docker Compose 기반으로 배포
- 서비스 구성요소:
  - **Spring Boot 애플리케이션**
  - **Kafka / Zookeeper / Redis / PostgreSQL / MongoDB** (모두 Docker Container로 구동)
- **GitHub Actions → EC2 자동 배포 파이프라인** 구축:
  - main 브랜치 푸시 시 자동 빌드, Docker Compose 재시작
  - `.env` 자동 생성 및 보안 삭제 처리
 
  
### 구성도
```
Developer → GitHub Actions → EC2 (Docker Compose)
                              ↳ bowchat-app
                              ↳ postgres
                              ↳ redis
                              ↳ kafka
                              ↳ mongodb
```
