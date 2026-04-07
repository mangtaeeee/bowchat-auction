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

    private static final String USER_CREATED_TOPIC = "user.created";

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void saveUserCreatedEvent(UserCreatedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.create(
                    USER_CREATED_TOPIC,
                    String.valueOf(event.userId()),
                    payload
            );
            outboxRepository.save(outboxEvent);
            log.debug("Outbox event saved: topic={}, userId={}", USER_CREATED_TOPIC, event.userId());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize user.created event", e);
        }
    }
}
