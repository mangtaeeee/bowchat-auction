package com.example.productservice.user.event;

public record UserCreatedEvent(
        Long userId,
        String email,
        String nickName
) {
}
