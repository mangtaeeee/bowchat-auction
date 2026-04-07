package com.example.auctionservice.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuctionTest {

    @Test
    void validateBidRejectsSellerBid() {
        // 판매자는 자신의 상품에 입찰할 수 없어야 한다.
        Auction auction = createAuction(10_000L, LocalDateTime.now().plusMinutes(10));

        assertThatThrownBy(() -> auction.validateBid(1L, 11_000L))
                .isInstanceOf(AuctionException.class)
                .extracting(ex -> ((AuctionException) ex).getErrorCode())
                .isEqualTo(AuctionErrorCode.SELLER_CANNOT_BID);
    }

    @Test
    void validateBidRejectsAmountLowerThanCurrentPrice() {
        // 현재가 이하 금액은 유효한 입찰로 인정하면 안 된다.
        Auction auction = createAuction(10_000L, LocalDateTime.now().plusMinutes(10));

        assertThatThrownBy(() -> auction.validateBid(2L, 10_000L))
                .isInstanceOf(AuctionException.class)
                .extracting(ex -> ((AuctionException) ex).getErrorCode())
                .isEqualTo(AuctionErrorCode.BID_TOO_LOW);
    }

    @Test
    void validateBidRejectsClosedAuction() {
        // 종료된 경매는 추가 입찰을 받지 않아야 한다.
        Auction auction = createAuction(10_000L, LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(() -> auction.validateBid(2L, 11_000L))
                .isInstanceOf(AuctionException.class)
                .extracting(ex -> ((AuctionException) ex).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_CLOSED);
    }

    @Test
    void placeBidUpdatesCurrentPriceAndWinner() {
        // 유효한 입찰이면 현재가와 최고 입찰자를 함께 갱신해야 한다.
        Auction auction = createAuction(10_000L, LocalDateTime.now().plusMinutes(10));

        auction.placeBid(2L, 15_000L, LocalDateTime.now());

        assertThat(auction.getCurrentPrice()).isEqualTo(15_000L);
        assertThat(auction.getWinner()).isEqualTo(2L);
    }

    private Auction createAuction(Long startingPrice, LocalDateTime endTime) {
        return Auction.builder()
                .product(100L)
                .sellerId(1L)
                .startTime(LocalDateTime.now().minusMinutes(1))
                .endTime(endTime)
                .startingPrice(startingPrice)
                .currentPrice(startingPrice)
                .build();
    }
}
