# Kafka 기반 실시간 경매 시스템

![Java](https://img.shields.io/badge/Java_17-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.x-6DB33F?style=flat-square&logo=spring-boot&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=flat-square&logo=apache-kafka&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-336791?style=flat-square&logo=postgresql&logoColor=white)

Kafka 기반으로 실시간 경매 및 채팅 기능을 구현하고,  
부하 테스트를 통해 병목을 분석하고 개선한 프로젝트입니다.

k6를 활용해 1000명 동시 입찰 상황을 시뮬레이션했고,  
HTTP 실패율을 73% → 58% → 0.42%까지 낮췄습니다.

---

## 주요 성과

| 지표 | 개선 전 | 1차 개선 | 2차 개선 |
|------|---------|---------|---------|
| HTTP 실패율 | 73% | 58% | 0.42% |
| Kafka Consumer Lag | 100+ | 40 이하 | 거의 0 |
| TPS | 불안정 | 40 유지 | 안정 유지 |
| CPU 사용률 | 0.07 | 0.4+ | 0.6+ |

---

## 프로젝트를 통해 학습한 내용

- Kafka Consumer 병렬 처리와 Lag 모니터링
- 부하 테스트 기반 병목 분석
- DLQ를 활용한 메시지 재처리 구조
- Partition / Consumer / Poll 전략 설계 경험

---

## 시스템 아키텍처

Client → WebSocket → Kafka Producer  
↓  
Topics  
↓  
SaveConsumer / BroadcastConsumer / BidConsumer  
↓  
MongoDB / WebSocket / PostgreSQL + Redis

---

## 기술 스택

- Java 17
- Spring Boot 3.x
- PostgreSQL, MongoDB
- Redis
- Kafka
- Prometheus, Grafana
- k6
- Docker

---

## 성능 개선 과정

### 1. 문제 상황

- HTTP 실패율 73%
- Kafka Consumer Lag 증가
- DB 커넥션 풀 한계 도달

단일 Consumer와 제한된 자원으로 인해  
요청이 몰리는 순간 처리 속도가 따라가지 못하는 구조였다.

---

### 2. 1차 개선

#### Kafka Consumer 병렬 처리

@KafkaListener(
topics = "auction-bid",
concurrency = "4"
)

- Consumer 병렬 처리 적용
- DB Pool 확장 (10 → 30)
- Thread Pool 확장
- Kafka Producer / Consumer 튜닝

#### 결과

- HTTP 실패율: 73% → 58%
- Kafka Lag 감소

다만 트래픽이 몰리는 구간에서는 여전히 밀림이 발생했다.

---

### 3. 2차 개선

1차 개선 이후에도 Lag가 순간적으로 증가하고  
DLQ가 반복적으로 쌓이는 문제가 있었다.

#### 문제

- Partition 수에 따른 병렬 처리 상한
- batch 처리로 인한 대량 retry
- fetch 설정으로 인한 처리 지연

#### 상세 원인

- Partition: 4 / concurrency: 4 → 병렬 처리 상한 고정
- max-poll-records: 1000 → 실패 시 전체 재처리
- fetch-min-size: 1MB → 메시지 대기 발생

실제 로그에서도 retry → rollback → DLQ 흐름이 반복됐다.

---

#### 개선

- Partition: 4 → 6
- concurrency: 4 → 6
- max-poll-records: 1000 → 500
- fetch-min-size: 1MB → 512KB
- fetch-max-wait: 1000 → 500

배포 환경이 2 Core라서  
무작정 늘리지 않고 안정적으로 처리 가능한 수준으로 조정했다.

---

#### 결과

- HTTP Error Rate: 0.42%
- Kafka Lag: 거의 0
- DLQ: 간헐적 발생
- 처리 상태 안정화

Lag를 줄이기보다  
처음부터 쌓이지 않도록 구조를 바꾼 것이 핵심이었다.

---

## MSA 구조로의 개선

기존 구조는 Kafka를 사용하고 있지만  
하나의 애플리케이션 안에서 모든 기능이 처리되는 형태였다.

### 문제

- 채팅 / 경매 / 입찰 기능이 강하게 결합
- 특정 기능 부하가 전체 서비스에 영향
- 부분 스케일 불가능

---

### 개선 방향

서비스를 역할 기준으로 분리했다.

- Auth Service
- Auction Service
- Bid Service
- Chat Service

Kafka는 단순 메시지 전달이 아니라  
서비스 간 이벤트 전달 역할로 사용했다.

---

### 구조

Client
↓
API Gateway

├─ Auth Service
├─ Auction Service
├─ Bid Service
├─ Chat Service

Kafka (Event Bus)

---

### 효과

- 서비스 단위로 확장 가능
- 장애 격리
- Consumer 역할 분리 명확화
- 이벤트 기반 구조로 전환

---

## 모니터링

Prometheus + Grafana를 통해 다음 지표를 확인했다.

- Kafka Consumer Lag
- HTTP Error Rate
- DB Active Connections
- JVM Memory / Thread

---

## 주요 기능

### 실시간 채팅
- WebSocket + Kafka 기반 메시지 전송

### 경매
- 입찰 이벤트 처리
- 실시간 가격 반영
- 낙관적 락(@Version) 적용

### 인증
- JWT 기반 인증
- Redis RefreshToken 관리

---

## 기술적 의사결정

### Kafka 사용 이유
- 비동기 이벤트 처리 구조 필요
- Consumer 분리를 통한 역할 기반 처리

### Redis 사용 이유
- 세션 / 토큰 캐싱
- 인증 성능 개선

### 동시성 제어
- Kafka Partition Key 기반 순서 보장
- JPA @Version 사용

### DLQ
- 실패 메시지 분리 및 재처리

---

## 인프라

- AWS EC2
- Docker Compose
- Kafka / Redis / PostgreSQL / MongoDB 구성

---

## 실행 방법

git clone https://github.com/mangtaeeee/bowchat-auction.git  
cd bowchat-auction  
docker-compose up -d

---

## 관련 글

- https://kimmangtae.tistory.com/35
- https://kimmangtae.tistory.com/38

---

## Contact

- Email: osp9658@gmail.com
- Blog: https://kimmangtae.tistory.com
- GitHub: https://github.com/mangtaeeee
