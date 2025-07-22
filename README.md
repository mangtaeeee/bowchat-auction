
# BowChat - SNS 기반 실시간 채팅을 통한 경매 

![Java](https://img.shields.io/badge/Java-17-007396?logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot)
![MongoDB](https://img.shields.io/badge/MongoDB-7.0-47A248?logo=mongodb)
![Kafka](https://img.shields.io/badge/Kafka-3.x-231F20?logo=apachekafka)
![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?logo=redis)
![Docker](https://img.shields.io/badge/Docker-20.10-2496ED?logo=docker)
![License](https://img.shields.io/badge/license-MIT-green)

---

Spring Boot, Kafka, MongoDB, Redis, WebSocket 기반의 **SNS 로그인 기반 실시간 채팅 및 경매 애플리케이션**입니다.  
JWT 기반 인증과 OAuth2 소셜 로그인(Google, Kakao, Naver)을 지원하며, Kafka Topic 분리 설계로 채팅/이벤트/경매 메시지를 독립적으로 처리합니다.  
Redis 캐시를 통해 세션 관리 최적화를 구현했습니다.

---

## 🏗 시스템 아키텍처

- WebSocket(STOMP) + Kafka 메시지 브로커 + Redis 세션 캐싱
- 메시지 처리 구조:
  ```
  Client → WebSocket → Kafka Producer
                  ↳ chat-message → SaveConsumer, BroadcastConsumer
                  ↳ chat-event   → EventConsumer
                  ↳ auction-bid  → BidConsumer
  ```
- 인증 흐름: OAuth2 → JWT 발급 → Redis RefreshToken 관리
- MongoDB를 채팅 로그 저장소로 활용, RDB는 메타데이터 관리에 사용 예정
- Topic 및 Consumer 그룹 설계로 고부하 상황에서도 안정적 메시지 처리

---

## 주요 기능

- JWT 기반 회원가입/로그인
- OAuth2 소셜 로그인 (Google, Kakao, Naver)
- Kafka Pub-Sub 기반 메시지 브로커
- WebSocket 실시간 채팅
- MongoDB 채팅 로그 저장
- Redis 세션/토큰 캐시
- 채팅방 입장/퇴장 이벤트 처리 자동화
- 경매 메시지(AUCTION_BID, AUCTION_END) Topic 분리 및 낙찰 처리 준비
- Docker Compose 기반 개발 환경 제공

---

## 📦 기술 스택

| 구분          | 기술                          |
|---------------|---------------------------------|
| Language      | Java 17                        |
| Framework     | Spring Boot 3.x, Spring Security|
| Database      | MongoDB, H2 (메모리 DB)        |
| Message Queue | Apache Kafka                   |
| Cache         | Redis                          |
| DevOps        | Docker, Docker Compose         |

---

## 📁 프로젝트 구조

```
src/main/java/com/example/bowchat
├── auth                # 인증 도메인 (JWT + OAuth2)
├── chatmessage         # 채팅 메시지 도메인 (Kafka Consumer)
├── chatroom            # 채팅방 도메인
├── kafka               # Kafka Producer/Consumer/Config
├── websocket           # WebSocket 핸들러 및 세션 관리
├── config              # 보안/캐시/Kafka/WebSocket 설정
├── global              # 공통 처리 (예외, 유틸)
└── BowchatApplication  # 메인 애플리케이션
```

---

## ⚙️ Kafka Topic 설계

| Topic 이름      | 메시지 타입                  | Consumer               |
|-----------------|-------------------------------|------------------------|
| `chat-message`  | `CHAT`, `FILE`                | SaveConsumer, BroadcastConsumer |
| `chat-event`    | `ENTER`, `LEAVE`, `SYSTEM`    | EventConsumer          |
| `auction-bid`   | `AUCTION_BID`, `AUCTION_END`  | BidConsumer            |

---

## 🚀 로컬 개발환경 (Docker Compose)

### 사전 준비
- Docker
- Docker Compose
- Java 17
- Gradle 8.x

### 실행
1. Docker Compose 실행
    ```bash
    docker-compose up -d
    ```
2. Spring Boot 실행 (Active Profile: `dev`)

---

### 환경 변수 (.env)
```
JWT_SECRET=your_jwt_secret_key
OAUTH_CLIENT_ID=your_client_id
OAUTH_CLIENT_SECRET=your_client_secret
MONGODB_URI=mongodb://localhost:27017/chatdb
REDIS_HOST=localhost
```

---

## 📑 API 문서
Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

---

## 🗺 향후 계획

- 경매 기능 완성 (입찰 처리 및 낙찰 브로드캐스트)
- AWS EC2 배포 및 S3 이미지 업로드
- GitHub Actions 기반 CI/CD 파이프라인 구축
- Prometheus + Grafana 모니터링
