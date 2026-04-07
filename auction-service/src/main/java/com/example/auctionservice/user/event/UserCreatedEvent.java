package com.example.auctionservice.user.event;

import java.time.Instant;

public record UserCreatedEvent(
        String eventId,
        Long userId,
        String email,
        String nickName,
        Instant occurredAt
) {
}
