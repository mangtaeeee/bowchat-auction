package com.example.chatservice.chatroom.client.dto;

import java.time.LocalDateTime;

public record AuctionInfo(
        Long id,
        Long productId,
        Long currentPrice,
        LocalDateTime endTime,
        boolean closed
) {
}
