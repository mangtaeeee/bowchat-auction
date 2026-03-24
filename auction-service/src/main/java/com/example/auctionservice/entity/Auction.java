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

    private Long product;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Long startingPrice;

    @Column(nullable = false)
    private Long currentPrice;

    private Long winner;

    public void start(LocalDateTime now) {
        this.startTime    = now;
        this.currentPrice = this.startingPrice;
    }

    public void placeBid(Long bidder, Long amount, LocalDateTime now) {
        if (now.isAfter(endTime)) {
            throw new IllegalStateException("이미 경매가 종료되었습니다.");
        }
        if (amount <= currentPrice) {
            throw new IllegalArgumentException("입찰가가 현재 최고가 이하입니다.");
        }
        this.currentPrice = amount;
        this.winner = bidder;
    }

    public void validateBid(Long bidderId, Long bidAmount) {
        if (this.getProduct().equals(bidderId)) {
//            throw new AuctionException(AuctionErrorCode.SELLER_CANNOT_BID);
        }
        if (bidAmount <= this.getCurrentPrice()) {
//            throw new AuctionException(AuctionErrorCode.BID_TOO_LOW);
        }
    }

    public boolean isClosed(LocalDateTime now) {
        return now.isAfter(endTime);
    }

    public static Auction of(Long product,Long startingPrice, LocalDateTime endTime) {
        return Auction.builder()
                .product(product)
                .startTime(LocalDateTime.now())
                .endTime(endTime)
                .startingPrice(startingPrice)
                .currentPrice(startingPrice)
                .build();
    }
}
