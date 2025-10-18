# BowChat - 실시간 채팅 및 경매 플랫폼

![Java](https://img.shields.io/badge/Java-17-007396?logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-7.0-47A248?logo=mongodb)
![Kafka](https://img.shields.io/badge/Kafka-3.x-231F20?logo=apachekafka)
![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?logo=redis)
![Docker](https://img.shields.io/badge/Docker-20.10-2496ED?logo=docker)

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
- **실시간 채팅**: WebSocket + Kafka
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
| Monitoring | Prometheus, Grafana |
| Load Test  | k6 |
| DevOps     | Docker, Docker Compose, GitHub Actions |
| ORM/Query  | JPA, QueryDSL |

---

## 부하 테스트 및 성능 개선

### 테스트 목표
1000명의 사용자가 동시에 입찰 요청을 보낼 때 발생하는 **Kafka·DB 병목 구간을 식별하고 성능을 개선**하는 것.  
실제 서비스 트래픽 급등 상황(경매 마감 직전)을 시뮬레이션했습니다.

### 테스트 환경
- AWS EC2 (c7i-flex.large / 2vCPU / 4GiB RAM)
- Docker Compose 기반 배포
- Prometheus + Grafana 실시간 모니터링

### 테스트 시나리오 (k6)
- WebSocket을 통해 1000명의 사용자가 동시에 `AUCTION_BID` 이벤트 전송  
- 서버는 Kafka Producer → Consumer → PostgreSQL/Redis 순서로 처리  
- 부하 단계별 VU 증가:
  ```js
  stages: [
    { duration: "5s", target: 100 },
    { duration: "10s", target: 500 },
    { duration: "10s", target: 1000 },
    { duration: "20s", target: 1000 },
    { duration: "5s", target: 0 },
  ]
  ```

---

### 1차 테스트 결과
| 항목 | 결과 |
|------|------|
| HTTP 실패율 | **73%** |
| Kafka Consumer Lag | **100+** 큐 적체 |
| DB Active Connections | 풀 한계(10)에 도달 |
| Transactions/sec | 폭주 후 급락 |
| CPU Usage | 0.07 수준 (리소스 여유) |

**결론:** 단일 Consumer 스레드와 제한된 DB 커넥션 풀로 인한 병목 발생.  
Kafka 큐 적체 → DB 대기 → 응답 실패율 증가로 이어짐.

---

### 1차 개선
- **@KafkaListener(concurrency=4)** 적용 → Consumer 병렬 처리
- **Kafka 토픽 파티션 자동 확장 스크립트 추가 (`kafka-init.sh`)**
- **DB Connection Pool 확장 (10 → 30)**  
- **Tomcat Thread 확장 (max 400)**  

**결과:** 실패율 73% → 62%, Kafka Lag 완화, 처리 속도 향상.

---

### ⚡ 2차 개선
Kafka Producer/Consumer 설정을 튜닝하여 메시지 전송 효율 최적화.

```yaml
spring:
  kafka:
    producer:
      acks: all
      linger-ms: 5
      batch-size: 32768
      buffer-memory: 67108864
      compression-type: lz4
    consumer:
      enable-auto-commit: false
      max-poll-records: 1000
      fetch-min-size: 1048576
      fetch-max-wait: 1000
```

**결과:**
| 항목 | 개선 전 | 개선 후 |
|------|----------|----------|
| HTTP 실패율 | 73% | **58% (-15%)** |
| Kafka Consumer Lag | 100+ | **40 이하 안정화** |
| DB Connection | 20~25 | **20 이하 유지** |
| CPU Usage | 0.07 | **0.4 이상 활용** |
| 처리 안정성 | 폭주 후 급락 | **40 TPS 전후 안정화** |

---

### Grafana 대시보드 모니터링 항목
- Kafka Consumer Lag
- PostgreSQL Active Connections
- Transactions/sec
- JVM Threads / Memory Usage
- HTTP Error Rate (%)

Prometheus → Grafana 연동을 통해 실시간으로 병목 구간과 개선 효과를 시각적으로 검증.

---

## Why? 기술 선택 배경

### Redis 사용 이유
- **세션/토큰 캐싱**으로 WebSocket 연결 안정성 확보  
- In-memory 캐시로 로그인 및 인증 속도 향상  
- Kafka와 역할 분리 (Session vs Stream)

### STOMP → Kafka 전환 이유
- STOMP는 단일 서버 기반 구조로 확장성 한계 존재  
- Kafka 도입으로 서버 간 메시지 일관성 및 순서 보장 확보  
- Topic 단위 Consumer 분리로 병렬 처리 및 재처리 가능

### Kafka + Optimistic Lock 병행 이유
- **Kafka 파티션 키 (auctionId)** 로 경매 단위 순차 처리 보장  
- **JPA @Version** 으로 중복 커밋/충돌 시 정합성 보장  

---

## Kafka 실패 처리 (DLQ 구조)
Kafka `DefaultErrorHandler + DeadLetterPublishingRecoverer` 로  
**실패 메시지를 DLT(Dead Letter Topic)** 으로 분리해 재처리 가능.

```java
WARN  KafkaConsumerConfig : DLQ 전송: topic=auction-bid.DLT, reason=OptimisticLockingFailureException
INFO  KafkaConsumerConfig : Kafka 재시도 1회 실패: key=AUCTION:1
```

---

## 배포 및 인프라 구성
- AWS EC2 + Docker Compose 기반 서비스 구성  
- GitHub Actions CI/CD 자동 배포  
- 주요 컨테이너:
  - Spring Boot, Kafka, Redis, PostgreSQL, MongoDB, Prometheus, Grafana

```
Developer → GitHub Actions → EC2 (Docker Compose)
                              ↳ bowchat-app
                              ↳ postgres
                              ↳ redis
                              ↳ kafka
                              ↳ mongodb
                              ↳ prometheus
                              ↳ grafana
```

---

## 성능 개선 요약
| 항목 | 개선 전 | 개선 후 | 효과 |
|------|----------|----------|------|
| HTTP 실패율 | 73% | **58%** | 요청 성공률 +15% |
| Kafka Lag | 100+ | **40 이하** | 메시지 지연 완화 |
| DB Connection | 10 | **30 확장** | 대기열 제거 |
| TPS | 불안정 | **40 안정적 유지** | 처리율 향상 |
| CPU 활용 | 0.07 | **0.4 이상** | 병렬 처리 효율 상승 |

---
