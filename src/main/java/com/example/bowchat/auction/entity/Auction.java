package com.example.bowchat.auction.entity;

import com.example.bowchat.product.entity.Product;
import com.example.bowchat.user.entity.User;
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID", nullable = false)
    private Product product;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Long startingPrice;

    @Column(nullable = false)
    private Long currentPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WINNER_ID")
    private User winner;

    public void start(LocalDateTime now) {
        this.startTime    = now;
        this.currentPrice = this.startingPrice;
    }

    public void placeBid(User bidder, Long amount, LocalDateTime now) {
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
        if (this.getProduct().getSeller().getId().equals(bidderId)) {
            throw new AuctionException(AuctionErrorCode.SELLER_CANNOT_BID);
        }
        if (bidAmount <= this.getCurrentPrice()) {
            throw new AuctionException(AuctionErrorCode.BID_TOO_LOW);
        }
    }

    public boolean isClosed(LocalDateTime now) {
        return now.isAfter(endTime);
    }


    public static Auction of(Product product, LocalDateTime endTime) {
        return Auction.builder()
                .product(product)
                .startTime(LocalDateTime.now())
                .endTime(endTime)
                .startingPrice(product.getStartingPrice())
                .currentPrice(product.getStartingPrice())
                .build();
    }
}
