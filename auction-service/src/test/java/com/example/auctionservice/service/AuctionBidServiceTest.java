package com.example.auctionservice.service;

import com.example.auctionservice.entity.Auction;
import com.example.auctionservice.entity.AuctionErrorCode;
import com.example.auctionservice.entity.AuctionException;
import com.example.auctionservice.repository.AuctionBidRepository;
import com.example.auctionservice.repository.AuctionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionBidServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private AuctionBidRepository auctionBidRepository;

    @InjectMocks
    private AuctionBidService auctionBidService;

    @Test
    void placeBidTranslatesOptimisticLockConflictToAuctionException() {
        // JPA 낙관적 락 예외를 그대로 노출하지 않고 도메인 예외로 번역해야 API 응답을 일관되게 만들 수 있다.
        Auction auction = Auction.builder()
                .id(1L)
                .product(10L)
                .sellerId(100L)
                .startTime(LocalDateTime.now().minusMinutes(1))
                .endTime(LocalDateTime.now().plusMinutes(10))
                .startingPrice(1_000L)
                .currentPrice(1_000L)
                .build();

        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));
        when(auctionRepository.saveAndFlush(any(Auction.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Auction.class, 1L));

        assertThatThrownBy(() -> auctionBidService.placeBid(1L, 200L, 2_000L))
                .isInstanceOf(AuctionException.class)
                .extracting(ex -> ((AuctionException) ex).getErrorCode())
                .isEqualTo(AuctionErrorCode.CONCURRENT_BID_CONFLICT);

        verify(auctionBidRepository, never()).save(any());
    }
}
