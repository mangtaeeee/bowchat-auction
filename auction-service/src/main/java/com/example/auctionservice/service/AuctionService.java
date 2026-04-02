package com.example.auctionservice.service;


import com.example.auctionservice.client.ProductServiceClient;
import com.example.auctionservice.dto.request.StartAuctionRequest;
import com.example.auctionservice.dto.response.AuctionResponse;
import com.example.auctionservice.entity.Auction;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionService {

    private final AuctionBidService auctionBidService;
    private final AuctionRepository auctionRepository;
    private final ChatProducer chatProducer;
    private final UserQueryService userQueryService;
    private final ProductServiceClient productServiceClient;

    public void placeBidAndBroadcast(Long auctionId, Long bidderId, Long bidAmount) {
        auctionBidService.placeBid(auctionId, bidderId, bidAmount); // 커밋됨
        String nickname = userQueryService.getUser(bidderId).getNickname();
        sendBroadcast(auctionId, bidderId, nickname, bidAmount);
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
    @Transactional(readOnly = true)
    public AuctionResponse findAuctionByProductId(Long productId) {
        Auction auction = auctionRepository.findByProduct(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "해당 상품의 경매를 찾을 수 없습니다."));
        return AuctionResponse.of(auction);
    }

    @Transactional(readOnly = true)
    public List<AuctionResponse> getAllAuctions() {
        return auctionRepository.findAll().stream()
                .map(AuctionResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
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
