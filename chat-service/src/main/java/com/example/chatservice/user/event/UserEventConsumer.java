package com.example.chatservice.user.event;

import com.example.chatservice.user.service.UserCreatedEventProcessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private static final String USER_CREATED_TOPIC = "user.created";

    private final UserCreatedEventProcessor userCreatedEventProcessor;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = USER_CREATED_TOPIC, groupId = "chat-service-group")
    public void handleUserCreated(String message) {
        UserCreatedEvent event = parseEvent(message);

        if (!userCreatedEventProcessor.process(event)) {
            log.info("Duplicate {} ignored: eventId={}, userId={}", USER_CREATED_TOPIC, event.eventId(), event.userId());
            return;
        }

        log.info("UserSnapshot saved from {}: eventId={}, userId={}", USER_CREATED_TOPIC, event.eventId(), event.userId());
    }

    private UserCreatedEvent parseEvent(String message) {
        try {
            String innerJson = objectMapper.readValue(message, String.class);
            return objectMapper.readValue(innerJson, UserCreatedEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse user.created event message", e);
        }
    }
}
