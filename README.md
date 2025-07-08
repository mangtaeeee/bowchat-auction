
# BowChat - 실시간 채팅 시스템

![Java](https://img.shields.io/badge/Java-17-007396?logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot)
![MongoDB](https://img.shields.io/badge/MongoDB-7.0-47A248?logo=mongodb)
![Kafka](https://img.shields.io/badge/Kafka-3.x-231F20?logo=apachekafka)
![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?logo=redis)
![Docker](https://img.shields.io/badge/Docker-20.10-2496ED?logo=docker)
![License](https://img.shields.io/badge/license-MIT-green)

---

Spring Boot, MongoDB, Kafka, Redis, WebSocket 기반의 **실시간 채팅 애플리케이션**입니다.  
JWT 기반 인증과 OAuth2 소셜 로그인(Google, Kakao, Naver)을 지원하며, 사용자 관리 및 채팅방 관리 기능을 제공합니다.  
대규모 트래픽 대응을 위해 Kafka 메시지 브로커와 Redis 캐시를 적용했습니다.

---

## 시스템 아키텍처

![BowChat Architecture](https://user-images.githubusercontent.com/00000000/bowchat-architecture.png)

> **설계 포인트**
> - WebSocket(STOMP) + Kafka 메시지 브로커 + Redis 캐싱
> - 인증 흐름: OAuth2 → JWT 발급 → Redis RefreshToken 관리
> - 채팅 메시지 MongoDB 저장 및 H2로 채팅방 관리

---

## 주요 기능

- JWT 기반 회원가입/로그인
- OAuth2 소셜 로그인 (Google, Kakao, Naver)
- WebSocket 실시간 채팅
- Kafka 비동기 메시지 처리
- MongoDB 채팅 로그 저장
- Redis 캐시를 통한 세션 관리
- Swagger API 문서 제공
- Docker Compose 개발환경 제공

---

## 기술 스택

| 구분          | 기술                          |
|---------------|---------------------------------|
| Language      | Java 17                        |
| Framework     | Spring Boot 3.x, Spring Security|
| Database      | MongoDB, H2 (메모리 DB)        |
| Message Queue | Apache Kafka                   |
| Cache         | Redis                          |
| Protocol      | WebSocket (STOMP)              |
| DevOps        | Docker, Docker Compose         |

---

## 프로젝트 구조

```
src/main/java/com/example/bowchat
├── auth                # 인증 도메인 (JWT + OAuth2)
│   ├── controller
│   ├── dto
│   ├── jwt
│   ├── oauth
│   ├── repository
│   └── service
├── chatmessage         # 채팅 메시지 도메인
├── chatroom            # 채팅방 도메인
├── config              # 보안/캐시/웹소켓 설정
├── global              # 글로벌 공통 처리 (예외, 유틸)
└── BowchatApplication  # 메인 애플리케이션
```

---

## 로컬 개발환경 (Docker Compose)

### 사전 준비
- Docker
- Docker Compose
- Java 17
- Gradle 8.x

---

### Docker Compose 실행

1. 루트 디렉토리에 `docker-compose.yml` 파일 생성:
```yaml
version: '3.8'

services:
  mongodb:
    image: mongo:7.0
    container_name: bowchat-mongo
    ports:
      - "27017:27017"
    volumes:
      - ./data/mongo:/data/db

  kafka:
    image: bitnami/kafka:3.5
    container_name: bowchat-kafka
    ports:
      - "9092:9092"
    environment:
      - KAFKA_BROKER_ID=1
      - KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181
      - KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092
    depends_on:
      - zookeeper

  zookeeper:
    image: bitnami/zookeeper:3.8
    container_name: bowchat-zookeeper
    ports:
      - "2181:2181"

  redis:
    image: redis:7
    container_name: bowchat-redis
    ports:
      - "6379:6379"
```

2. 실행:
```bash
docker-compose up -d
```

---


---

### 환경 변수 설정
루트에 `.env` 파일 생성:
```
JWT_SECRET=your_jwt_secret_key
OAUTH_CLIENT_ID=your_client_id
OAUTH_CLIENT_SECRET=your_client_secret
```

---

## API 문서
Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

---

## 프로젝트 특징
- 도메인 중심 설계 (auth/chatmessage/chatroom)
- Kafka 비동기 메시징으로 대규모 트래픽 처리
- JWT + OAuth2 통합 인증 설계
- Docker Compose로 개발환경 손쉬운 재현

---

## 향후 계획
- AWS EC2 배포 및 S3 연동
- GitHub Actions 기반 CI/CD 파이프라인 구축
- Prometheus + Grafana 모니터링 추가
