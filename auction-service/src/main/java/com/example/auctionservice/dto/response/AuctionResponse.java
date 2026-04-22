package com.example.auctionservice.dto.response;

import com.example.auctionservice.entity.Auction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "경매 응답")
public record AuctionResponse(
        @Schema(description = "경매 ID", example = "1")
        Long id,
        @Schema(description = "상품 ID", example = "30")
        Long productId,
        @Schema(description = "현재 최고 입찰가", example = "15000")
        Long currentPrice,
        @Schema(description = "시작 가격", example = "10000")
        Long startingPrice,
        @Schema(description = "경매 시작 시각", example = "2099-01-01T09:00:00")
        LocalDateTime startTime,
        @Schema(description = "경매 종료 시각", example = "2099-01-01T10:00:00")
        LocalDateTime endTime,
        @Schema(description = "현재 낙찰 후보 사용자 ID", example = "2", nullable = true)
        Long winnerId,
        @Schema(description = "경매 종료 여부", example = "false")
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
