package com.example.auctionservice.service;


import com.example.auctionservice.client.ProductServiceClient;
import com.example.auctionservice.dto.request.StartAuctionRequest;
import com.example.auctionservice.dto.response.AuctionResponse;
import com.example.auctionservice.entity.Auction;
import com.example.auctionservice.entity.AuctionBid;
import com.example.auctionservice.repository.AuctionBidRepository;
import com.example.auctionservice.repository.AuctionRepository;
import com.example.auctionservice.user.service.UserQueryService;
import com.example.bowchat.kafkastarter.event.EventMessage;
import com.example.bowchat.kafkastarter.event.MessageType;
import com.example.bowchat.kafkastarter.producer.ChatProducer;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final AuctionBidRepository auctionBidRepository;
    private final ChatProducer chatProducer;
    private final UserQueryService userQueryService;
    private final ProductServiceClient productServiceClient;

    @Transactional
    public void placeBid(Long auctionId, Long bidderId, Long bidAmount) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다."));

        // 입찰 검증 (판매자 입찰 방지, 최고가 이하 방지, 종료 여부)
        auction.validateBid(bidderId, bidAmount);

        Long oldPrice = auction.getCurrentPrice();
        auction.placeBid(bidderId, bidAmount, LocalDateTime.now());
        auctionRepository.save(auction);

        saveBidHistory(auction, bidderId, bidAmount);

        log.info("입찰 완료: auctionId={}, bidderId={}, {}원 → {}원",
                auctionId, bidderId, oldPrice, bidAmount);

        // 입찰자 닉네임 조회 후 브로드캐스트
        String bidderNickname = userQueryService.getUser(bidderId).getNickname();
        sendBroadcast(auctionId, bidderId, bidderNickname, bidAmount);
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
    public void startAuction(Long productId, Long requestUserId, StartAuctionRequest request) {

        // 1. 상품 존재 여부 + 판매자 확인 (없으면 FeignException.NotFound)
        Long sellerId;
        try {
            sellerId = productServiceClient.getSellerId(productId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 상품입니다.");
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "상품 서비스에 접근할 수 없습니다.");
        }

        // 2. 판매자 본인 확인
        if (!sellerId.equals(requestUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "상품 판매자만 경매를 시작할 수 있습니다.");
        }

        Auction auction = Auction.of(productId, sellerId, request.startingPrice(), request.endTime());
        auctionRepository.save(auction);
        log.info("경매 시작: productId={}, sellerId={}", productId, sellerId);
    }

    public List<AuctionResponse> getAllAuctions() {
        return auctionRepository.findAll().stream()
                .map(AuctionResponse::of)
                .toList();
    }

    public AuctionResponse findAuctionById(Long id) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다."));
        return AuctionResponse.of(auction);
    }

    private void sendBroadcast(Long auctionId, Long bidderId, String bidderNickname, Long bidAmount) {
        EventMessage message = EventMessage.builder()
                .roomId(auctionId)        // auctionId = roomId로 매핑
                .senderId(bidderId)
                .senderName(bidderNickname)
                .topicName(MessageType.AUCTION_BID.getTopicName())
                .messageType(MessageType.AUCTION_BID.name())
                .content(String.valueOf(bidAmount))
                .timestamp(Instant.now().toEpochMilli())
                .build();

        chatProducer.send(message);
        log.info("입찰 브로드캐스트: auctionId={}, bidder={}, amount={}", auctionId, bidderNickname, bidAmount);
    }
}
