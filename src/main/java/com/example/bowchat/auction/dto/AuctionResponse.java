package com.example.bowchat.auction.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AuctionResponse(
        Long id,
        Long productId,
        String productName,
        Long currentPrice,
        LocalDateTime endTime
) {
}
