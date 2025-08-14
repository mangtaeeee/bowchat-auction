package com.example.bowchat.auction.dto;

import com.example.bowchat.auction.entity.Auction;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AuctionResponse(
        Long id,
        Long productId,
        String productName,
        Long currentPrice,
        LocalDateTime endTime,
        Long sellerId
) {
    public static AuctionResponse of(Auction auction) {
        return AuctionResponse.builder()
                .id(auction.getId())
                .productId(auction.getProduct().getId())
                .productName(auction.getProduct().getName())
                .currentPrice(auction.getCurrentPrice())
                .endTime(auction.getEndTime())
                .sellerId(auction.getProduct().getSeller().getId())
                .build();
    }
}
