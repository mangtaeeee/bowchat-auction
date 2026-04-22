package com.example.auctionservice.outbox;

import lombok.Builder;

@Builder
public record AuctionBidPlacedPayload(
        Long auctionId,
        Long bidderId,
        String bidderNickname,
        Long bidAmount,
        Long occurredAt
) {
    public static AuctionBidPlacedPayload of(
            Long auctionId,
            Long bidderId,
            String bidderNickname,
            Long bidAmount,
            Long occurredAt
    ) {
       return AuctionBidPlacedPayload.builder()
               .auctionId(auctionId)
               .bidderId(bidderId)
               .bidderNickname(bidderNickname)
               .bidAmount(bidAmount)
               .occurredAt(occurredAt)
               .build();
    }
}
