알겠습니다! Zookeeper와 Kafka에 대한 설명을 생략하고, 간단히 설정 방법만 안내하는 형식으로 다시 작성했습니다.

```markdown
# 채팅 시스템 설정 및 실행 가이드

이 프로젝트는 Spring Boot 기반의 채팅 시스템을 구현한 리포지토리입니다. MongoDB, Kafka, H2 메모리 데이터베이스를 사용하여 채팅방 시스템을 구축했습니다. 아래는 이 프로젝트를 로컬에서 설정하고 실행하는 방법입니다.

## 필수 환경

- **Homebrew**: macOS에서 패키지 관리 도구로 사용됩니다.
- **MongoDB**: 채팅 메시지를 저장하는 NoSQL 데이터베이스입니다.
- **Kafka**: 메시지 큐 시스템으로, 채팅 메시지를 효율적으로 처리합니다.

## 로컬 환경 설정

### 1. MongoDB 설정

MongoDB를 로컬에서 설정하는 방법은 아래와 같습니다.

#### MongoDB 설치 및 실행

1. **Homebrew 업데이트**  
   ```bash
   brew update
   ```

2. **MongoDB Tap 추가**  
   ```bash
   brew tap mongodb/brew
   ```

3. **MongoDB 설치**  
   ```bash
   brew install mongodb-community@7.0
   ```

4. **터미널에서 MongoDB 실행**  
   ```bash
   brew services start mongodb/brew/mongodb-community
   ```

5. **MongoDB 접속 확인**  
   ```bash
   mongosh
   use chatdb
   ```

6. **터미널에서 MongoDB 종료**  
   ```bash
   brew services stop mongodb/brew/mongodb-community
   ```

### 2. Kafka 설정

Kafka는 채팅 메시지를 처리하는 메시지 큐 시스템입니다.

#### Kafka 설치 및 실행

1. **Homebrew 업데이트**  
   ```bash
   brew update
   ```

2. **Kafka Tap 추가**  
   ```bash
   brew tap kafka/brew
   ```

3. **Kafka 설치**  
   ```bash
   brew install kafka
   ```

4. **터미널에서 Kafka 실행**  
   ```bash
   brew services start kafka
   ```

5. **Kafka 접속 확인**  
   ```bash
   kafka-topics.sh --list --bootstrap-server localhost:9092
   ```

6. **터미널에서 Kafka 종료**  
   ```bash
   brew services stop kafka
   ```

---
