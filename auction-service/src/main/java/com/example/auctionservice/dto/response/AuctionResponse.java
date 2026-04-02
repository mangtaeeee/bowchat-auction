package com.example.auctionservice.dto.response;

import com.example.auctionservice.entity.Auction;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AuctionResponse(
        Long id,
        Long productId,
        Long currentPrice,
        Long startingPrice,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long winnerId,
        boolean closed
) {
    public static AuctionResponse of(Auction auction) {
        return AuctionResponse.builder()
                .id(auction.getId())
                .productId(auction.getProduct())
                .currentPrice(auction.getCurrentPrice())
                .startingPrice(auction.getStartingPrice())
                .startTime(auction.getStartTime())
                .endTime(auction.getEndTime())
                .winnerId(auction.getWinner())
                .closed(auction.isClosed(LocalDateTime.now()))
                .build();
    }
}

