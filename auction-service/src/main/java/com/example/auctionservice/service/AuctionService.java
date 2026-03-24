package com.example.auctionservice.service;


import com.example.auctionservice.dto.AuctionResponse;
import com.example.auctionservice.dto.StartAuctionRequest;
import com.example.auctionservice.entity.Auction;
import com.example.auctionservice.entity.AuctionBid;
import com.example.auctionservice.repository.AuctionBidRepository;
import com.example.auctionservice.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final AuctionBidRepository auctionBidRepository;

    @Transactional
    public void placeBid(Long auctionId, Long bidderId, Long bidAmount) {
        // 경매 조회
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다."));

        // 입찰 검증
        auction.validateBid(bidderId, bidAmount);

        // 이전 가격 기록 (로깅용)
        Long oldPrice = auction.getCurrentPrice();

        // 입찰 처리
        auction.placeBid(bidderId, bidAmount, LocalDateTime.now());
        auctionRepository.save(auction);

        // 입찰 이력 저장
        saveBidHistory(auction, bidderId, bidAmount);

        // 로그
        log.info("입찰 완료: auctionId={}, bidderId={}, {}원 → {}원",
                auctionId, bidderId, oldPrice, bidAmount);

        // 브로드캐스트
        sendBroadCast(auction.getId(), bidderId, bidAmount);
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

    @Transactional
    public void startAuction(Long productId, StartAuctionRequest request) {
        Auction auction = Auction.of(productId, request.startingPrice(), request.endTime());
        auctionRepository.save(auction);
    }

    public List<AuctionResponse> getAllAuctions() {
        return auctionRepository.findAll().stream()
                .map(AuctionResponse::of)
                .toList();
    }

    private void sendBroadCast(Long auctionId, Long userId, Long bidAmount) {
        chatProducer.send(ChatEventFactory.auctionBid(auctionId, userId, bidAmount));
    }

    public AuctionResponse findAuctionById(Long id) {
        Auction auction = auctionRepository.findWithProductAndSellerById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다."));
        return AuctionResponse.of(auction);
    }
}
