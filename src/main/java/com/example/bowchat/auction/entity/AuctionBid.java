package com.example.bowchat.auction.entity;

import com.example.bowchat.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUCTION_BIDS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuctionBid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어느 경매의 입찰인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AUCTION_ID", nullable = false)
    private Auction auction;

    // 누가 입찰했는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BIDDER_ID", nullable = false)
    private User bidder;

    // 입찰 금액
    @Column(nullable = false)
    private Long amount;

    // 입찰 시각
    @Column(nullable = false)
    private LocalDateTime bidTime;
}