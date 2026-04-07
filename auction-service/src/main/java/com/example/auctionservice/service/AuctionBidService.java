package com.example.auctionservice.service;

import com.example.auctionservice.dto.request.StartAuctionRequest;
import com.example.auctionservice.entity.Auction;
import com.example.auctionservice.entity.AuctionBid;
import com.example.auctionservice.entity.AuctionErrorCode;
import com.example.auctionservice.entity.AuctionException;
import com.example.auctionservice.repository.AuctionBidRepository;
import com.example.auctionservice.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionBidService {

    private final AuctionRepository auctionRepository;
    private final AuctionBidRepository auctionBidRepository;

    @Transactional
    public void placeBid(Long auctionId, Long bidderId, Long bidAmount) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND));

        auction.validateBid(bidderId, bidAmount);
        auction.placeBid(bidderId, bidAmount, LocalDateTime.now());

        try {
            auctionRepository.saveAndFlush(auction);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("낙관적 락 충돌: auctionId={}, bidderId={}, bidAmount={}", auctionId, bidderId, bidAmount);
            throw new AuctionException(AuctionErrorCode.CONCURRENT_BID_CONFLICT);
        }

        saveBidHistory(auction, bidderId, bidAmount);
    }

    @Transactional
    public void createAuction(Long productId, Long sellerId, StartAuctionRequest request) {
        Auction auction = Auction.of(productId, sellerId, request.startingPrice(), request.endTime());
        auctionRepository.save(auction);
        log.info("경매 시작: productId={}, sellerId={}", productId, sellerId);
    }

    private void saveBidHistory(Auction auction, Long bidder, Long bidAmount) {
        auctionBidRepository.save(
                AuctionBid.builder()
                        .auction(auction)
                        .bidder(bidder)
                        .amount(bidAmount)
                        .bidTime(LocalDateTime.now())
                        .build()
        );
    }
}
