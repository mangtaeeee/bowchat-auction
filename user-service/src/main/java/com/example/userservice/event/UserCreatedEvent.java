package com.example.userservice.event;

import com.example.userservice.entity.User;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record UserCreatedEvent(
        String eventId,
        Long userId,
        String email,
        String nickName,
        Instant occurredAt
) {
    public static UserCreatedEvent of(User user) {
        return UserCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(user.getId())
                .email(user.getEmail())
                .nickName(user.getNickname())
                .occurredAt(Instant.now())
                .build();
   }
}
