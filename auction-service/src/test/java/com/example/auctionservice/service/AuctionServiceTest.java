package com.example.auctionservice.service;

import com.example.auctionservice.client.ProductServiceClient;
import com.example.auctionservice.dto.request.StartAuctionRequest;
import com.example.auctionservice.dto.response.AuctionResponse;
import com.example.auctionservice.entity.Auction;
import com.example.auctionservice.entity.AuctionErrorCode;
import com.example.auctionservice.entity.AuctionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock
    private AuctionBidService auctionBidService;

    @Mock
    private com.example.auctionservice.repository.AuctionRepository auctionRepository;

    @Mock
    private ProductServiceClient productServiceClient;

    @InjectMocks
    private AuctionService auctionService;

    @Test
    void startAuctionCreatesAuctionWhenRequesterIsSeller() {
        // 판매자가 직접 시작 요청한 경우에만 경매 생성으로 이어져야 한다.
        StartAuctionRequest request = new StartAuctionRequest(10_000L, LocalDateTime.of(2026, 4, 6, 18, 0));
        when(productServiceClient.getSellerId(3L)).thenReturn(7L);

        auctionService.startAuction(3L, 7L, request);

        verify(auctionBidService).createAuction(3L, 7L, request);
    }

    @Test
    void startAuctionRejectsRequesterWhoIsNotSeller() {
        // 판매자가 아닌 사용자는 시작 단계에서 바로 차단해야 한다.
        StartAuctionRequest request = new StartAuctionRequest(10_000L, LocalDateTime.of(2026, 4, 6, 18, 0));
        when(productServiceClient.getSellerId(3L)).thenReturn(99L);

        assertThatThrownBy(() -> auctionService.startAuction(3L, 7L, request))
                .isInstanceOf(AuctionException.class)
                .extracting(ex -> ((AuctionException) ex).getErrorCode())
                .isEqualTo(AuctionErrorCode.ONLY_SELLER_CAN_START_AUCTION);

        verifyNoInteractions(auctionBidService);
    }

    @Test
    void placeBidReturnsUpdatedAuctionStateForImmediateUiRefresh() {
        // 입찰 성공 응답에 최신 경매 상태를 담아주면 프론트가 브로드캐스트를 기다리지 않고 바로 화면을 갱신할 수 있다.
        Auction auction = Auction.builder()
                .id(9L)
                .product(30L)
                .sellerId(1L)
                .startTime(LocalDateTime.now().minusMinutes(1))
                .endTime(LocalDateTime.now().plusMinutes(10))
                .startingPrice(10_000L)
                .currentPrice(15_000L)
                .winner(5L)
                .build();
        when(auctionBidService.placeBid(9L, 5L, "bidder", 15_000L)).thenReturn(auction);

        AuctionResponse response = auctionService.placeBid(9L, 5L, "bidder", 15_000L);

        assertThat(response.id()).isEqualTo(9L);
        assertThat(response.currentPrice()).isEqualTo(15_000L);
        assertThat(response.winnerId()).isEqualTo(5L);
        verify(auctionBidService).placeBid(9L, 5L, "bidder", 15_000L);
    }
}
