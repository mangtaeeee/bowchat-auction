package com.example.auctionservice.user.event;

public record UserCreatedEvent(
        Long userId,
        String email,
        String nickName
) {
}
