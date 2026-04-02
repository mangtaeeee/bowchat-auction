package com.example.chatservice.user.event;

import com.example.chatservice.user.entity.UserSnapshot;
import com.example.chatservice.user.service.UserSnapshotSaver;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final UserSnapshotSaver userSnapshotSaver;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user.created", groupId = "chat-service-group")
    public void handleUserCreated(String message) {
        try {
            // 1. 바깥 따옴표 벗겨서 실제 JSON 문자열 추출
            String innerJson = objectMapper.readValue(message, String.class);
            // 2. 실제 JSON을 UserCreatedEvent로 변환
            UserCreatedEvent event = objectMapper.readValue(innerJson, UserCreatedEvent.class);

            log.info("user.created 수신: userId={}", event.userId());
            userSnapshotSaver.saveIfAbsent(UserSnapshot.builder()
                    .userId(event.userId())
                    .email(event.email())
                    .nickname(event.nickName())
                    .build());

            log.info("UserSnapshot 저장 완료: userId={}", event.userId());
        } catch (Exception e) {
            log.error("user.created 처리 실패: {}", message, e);
            throw new RuntimeException(e);
        }
    }
}
