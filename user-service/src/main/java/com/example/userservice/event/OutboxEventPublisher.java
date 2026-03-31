package com.example.userservice.event;

import com.example.userservice.event.outbox.OutboxEvent;
import com.example.userservice.event.outbox.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    // 트랜잭션 안에서 호출 - DB에 이벤트 저장만 함 (Kafka 발행 X)
    public void saveUserCreatedEvent(UserCreatedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.create(
                    "user.created",
                    String.valueOf(event.userId()),
                    payload
            );
            outboxRepository.save(outboxEvent);
            log.debug("outbox 저장 완료: userId={}", event.userId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("이벤트 직렬화 실패: " + e.getMessage(), e);
        }
    }
}
