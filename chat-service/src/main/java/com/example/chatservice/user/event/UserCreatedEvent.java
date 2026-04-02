package com.example.chatservice.user.event;

public record UserCreatedEvent(
        Long userId,
        String email,
        String nickName
) {
}
