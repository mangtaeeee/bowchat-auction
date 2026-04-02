package com.example.auctionservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUCTIONS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    // product-service의 productId만 저장
    private Long product;

    // seller는 product-service에서 관리, auctionId로 조회
    private Long sellerId;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Long startingPrice;

    @Column(nullable = false)
    private Long currentPrice;

    private Long winner;

    public void placeBid(Long bidder, Long amount, LocalDateTime now) {
        if (now.isAfter(endTime)) {
            throw new AuctionException(AuctionErrorCode.AUCTION_CLOSED);
        }
        if (amount <= currentPrice) {
            throw new AuctionException(AuctionErrorCode.BID_TOO_LOW);
        }
        this.currentPrice = amount;
        this.winner = bidder;
    }

    public void validateBid(Long bidderId, Long bidAmount) {
        if (this.sellerId != null && this.sellerId.equals(bidderId)) {
            throw new AuctionException(AuctionErrorCode.SELLER_CANNOT_BID);
        }
        if (bidAmount <= this.currentPrice) {
            throw new AuctionException(AuctionErrorCode.BID_TOO_LOW);
        }
        if (isClosed(LocalDateTime.now())) {
            throw new AuctionException(AuctionErrorCode.AUCTION_CLOSED);
        }
    }

    public boolean isClosed(LocalDateTime now) {
        return now.isAfter(endTime);
    }

    public static Auction of(Long productId, Long sellerId, Long startingPrice, LocalDateTime endTime) {
        return Auction.builder()
                .product(productId)
                .sellerId(sellerId)
                .startTime(LocalDateTime.now())
                .endTime(endTime)
                .startingPrice(startingPrice)
                .currentPrice(startingPrice)
                .build();
    }
}
