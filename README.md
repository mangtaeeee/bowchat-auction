# Kafka 기반 실시간 경매 시스템

![Java](https://img.shields.io/badge/Java_17-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.x-6DB33F?style=flat-square&logo=spring-boot&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=flat-square&logo=apache-kafka&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-336791?style=flat-square&logo=postgresql&logoColor=white)

대규모 트래픽 환경에서 메시지 정합성, 동시성 제어, 장애 복구를 학습하기 위한 프로젝트입니다.  
k6 부하테스트로 1000명 동시 접속을 검증하고 요청 실패율을 73%에서 58%로 개선했습니다.

---

## 주요 성과

| 지표 | 개선 전 | 개선 후 | 효과 |
|------|---------|---------|------|
| **HTTP 실패율** | 73% | 58% | 요청 성공률 +15%p |
| **Kafka Consumer Lag** | 100+ | 40 이하 | 메시지 지연 완화 |
| **처리량 (TPS)** | 불안정 | 40 안정 유지 | 처리율 향상 |
| **CPU 활용률** | 0.07 | 0.4+ | 병렬 처리 효율 상승 |

---

## 프로젝트를 통해 학습한 내용

- Kafka Consumer 병렬 처리(concurrency) 및 Lag 모니터링
- k6 부하테스트를 통한 병목 구간 식별 및 개선
- Prometheus + Grafana 기반 실시간 모니터링 구축
- Dead Letter Queue(DLQ) 패턴으로 메시지 유실 방지
- Copilot + JUnit을 활용한 테스트 자동화 환경 구축

---

## 시스템 아키텍처

```
Client → WebSocket → Kafka Producer
                      ↓
                   Topics
                      ↓
         ┌────────────┼────────────┐
         ↓            ↓            ↓
    SaveConsumer  BroadcastConsumer  BidConsumer
         ↓            ↓            ↓
    MongoDB      WebSocket       PostgreSQL
                                  + Redis
```

**주요 구성 요소:**
- **WebSocket**: 실시간 양방향 통신
- **Kafka**: 메시지 스트리밍 및 역할별 Consumer 분리
- **PostgreSQL**: 사용자, 상품, 입찰 데이터 관리
- **MongoDB**: 채팅 로그 저장
- **Redis**: 세션/토큰 캐싱

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.x, Spring Security |
| Database | PostgreSQL, MongoDB |
| Cache | Redis |
| Messaging | Apache Kafka |
| Monitoring | Prometheus, Grafana |
| Load Test | k6 |
| DevOps | Docker, Docker Compose, GitHub Actions |
| ORM/Query | JPA, QueryDSL |

---

## 성능 개선 과정

### 1. 문제 상황

**테스트 시나리오**  
1000명의 사용자가 동시에 입찰 요청을 보내는 상황 시뮬레이션 (경매 마감 직전 트래픽 급등)

**테스트 환경**
- AWS EC2 (c7i-flex.large / 2vCPU / 4GiB RAM)
- Docker Compose 기반 배포
- Prometheus + Grafana 실시간 모니터링

**부하 테스트 설정 (k6)**
```javascript
stages: [
  { duration: "5s", target: 100 },
  { duration: "10s", target: 500 },
  { duration: "10s", target: 1000 },
  { duration: "20s", target: 1000 },
  { duration: "5s", target: 0 },
]
```

**개선 전 결과**

| 항목 | 결과 |
|------|------|
| HTTP 실패율 | 73% |
| Kafka Consumer Lag | 100+ 큐 적체 |
| DB Active Connections | 풀 한계(10)에 도달 |
| Transactions/sec | 폭주 후 급락 |
| CPU Usage | 0.07 (리소스 여유) |

**원인 분석**  
단일 Consumer 스레드와 제한된 DB 커넥션 풀로 인한 병목 발생.  
Kafka 큐 적체 → DB 대기 → 응답 실패율 증가로 이어짐.

---

### 2. 해결 방안

#### (1) Kafka Consumer 병렬 처리

**적용 내용**
```java
@KafkaListener(
    topics = "auction-bid",
    concurrency = "4"
)
public void consume(ConsumerRecord<String, AuctionBidEvent> record) {
    // 처리 로직
}
```

- Consumer 병렬 처리 활성화 (concurrency=4)
- Kafka 토픽 파티션 자동 확장 스크립트 추가 (kafka-init.sh)
- DB Connection Pool 확장 (10 → 30)
- Tomcat Thread 확장 (max 400)

**결과**  
실패율 73% → 62%, Kafka Lag 완화, 처리 속도 향상

---

#### (2) Kafka Producer/Consumer 튜닝

**설정 최적화**
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

**효과**
- Producer: 배치 처리 및 압축으로 전송 효율 향상
- Consumer: 폴링 최적화로 메시지 처리 속도 개선

---

### 3. 최종 결과

| 항목 | 개선 전 | 개선 후 | 개선율 |
|------|---------|---------|--------|
| HTTP 실패율 | 73% | 58% | -15%p |
| Kafka Consumer Lag | 100+ | 40 이하 | 안정화 |
| DB Active Connections | 20~25 | 20 이하 | 유지 |
| CPU Usage | 0.07 | 0.4+ | 활용률 상승 |
| 처리 안정성 | 폭주 후 급락 | 40 TPS 안정 유지 | 향상 |

---

## 부하 테스트 결과 스크린샷

### 개선 전
- HTTP 실패율: 73%
- Kafka Lag: 100+ 큐 적체
- CPU 활용률: 0.07 (병목)

### 개선 후
- HTTP 실패율: 58% (-15%p)
- Kafka Lag: 40 이하 안정화
- CPU 활용률: 0.4+ (병렬 처리)

### Grafana 대시보드
**모니터링 지표**
- Kafka Consumer Lag 실시간 추이
- PostgreSQL Active Connections
- HTTP Error Rate 추이
- JVM Threads / Memory Usage

---

## 주요 기능

### 1. 실시간 채팅
WebSocket + Kafka 기반 실시간 메시지 전송 및 저장

### 2. 경매 서비스
- 입찰/낙찰 이벤트 스트림 처리
- 실시간 가격 동기화
- 낙관적 락(@Version)을 통한 동시성 제어

### 3. 인증/보안
- OAuth2 소셜 로그인 (Google, Kakao, Naver)
- JWT 기반 토큰 인증
- Redis를 통한 RefreshToken 관리

### 4. 데이터 저장소
- **PostgreSQL**: 사용자 및 서비스 메타데이터
- **MongoDB**: 채팅 로그/이력
- **Redis**: 세션/토큰 캐시

### 5. 확장성
- Kafka Consumer 역할 분리 (저장, 브로드캐스트, 이벤트, 경매)
- 병렬 처리 구조
- Docker 기반 수평 확장 가능

---

## 기술적 의사결정

### Redis 선택 이유
- 세션/토큰 캐싱으로 WebSocket 연결 안정성 확보
- In-memory 캐시로 로그인 및 인증 속도 향상
- Kafka와 역할 분리 (Session vs Stream)

### STOMP 대신 Kafka 선택 이유
- STOMP는 단일 서버 기반 구조로 확장성 한계 존재
- Kafka 도입으로 서버 간 메시지 일관성 및 순서 보장 확보
- Topic 단위 Consumer 분리로 병렬 처리 및 재처리 가능

### 동시성 제어 방식
- Kafka 파티션 키(auctionId)로 경매 단위 순차 처리 보장
- JPA @Version으로 중복 커밋/충돌 시 정합성 보장
- 낙관적 락 실패 시 자동 재시도

### Dead Letter Queue(DLQ) 패턴
Kafka DefaultErrorHandler + DeadLetterPublishingRecoverer로  
실패 메시지를 DLT(Dead Letter Topic)으로 분리해 재처리 가능.

```
WARN KafkaConsumerConfig : DLQ 전송: topic=auction-bid.DLT, 
                            reason=OptimisticLockingFailureException
INFO KafkaConsumerConfig : Kafka 재시도 1회 실패: key=AUCTION:1
```

---

## 인프라 구성

**배포 환경**
- AWS EC2 + Docker Compose 기반 서비스 구성
- GitHub Actions CI/CD 자동 배포

**주요 컨테이너**
- Spring Boot
- Apache Kafka
- Redis
- PostgreSQL
- MongoDB
- Prometheus
- Grafana

**배포 플로우**
```
Developer → GitHub Actions → EC2 (Docker Compose)
                              ↓
                          Containers
                              ├─ bowchat-app
                              ├─ postgres
                              ├─ redis
                              ├─ kafka
                              ├─ mongodb
                              ├─ prometheus
                              └─ grafana
```

---

## 빠른 시작

### 사전 요구사항
```bash
# Docker & Docker Compose 설치 필요
docker --version
docker-compose --version
```

### 실행 방법
```bash
# 저장소 클론
git clone https://github.com/mangtaeeee/bowchat-auction.git
cd bowchat-auction

# Docker Compose로 모든 서비스 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f bowchat-app
```

### 접속 정보
- 애플리케이션: http://localhost:8080
- Grafana 대시보드: http://localhost:3000 (admin / admin)
- Prometheus: http://localhost:9090

### 부하 테스트 실행
```bash
# k6 설치 (Mac)
brew install k6

# 부하 테스트 실행
k6 run scripts/load-test.js
```

---

## 관련 블로그 포스팅

- [Kafka 기반 실시간 경매 시스템 부하 테스트 및 병목 개선 (k6 + Prometheus + Grafana)](https://kimmangtae.tistory.com/35)
- [Copilot + JUnit 테스트 코드 자동 생성 환경 구성](https://kimmangtae.tistory.com/34)

---

## Contact

- Email: osp9658@gmail.com
- Blog: https://kimmangtae.tistory.com
- GitHub: https://github.com/mangtaeeee
