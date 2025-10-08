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
