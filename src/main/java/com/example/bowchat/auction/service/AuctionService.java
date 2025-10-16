package com.example.bowchat.auction.service;

import com.example.bowchat.auction.dto.AuctionResponse;
import com.example.bowchat.auction.entity.Auction;
import com.example.bowchat.auction.entity.AuctionBid;
import com.example.bowchat.auction.repository.AuctionBidRepository;
import com.example.bowchat.auction.repository.AuctionRepository;
import com.example.bowchat.kafka.ChatEventFactory;
import com.example.bowchat.kafka.ChatProducer;
import com.example.bowchat.product.entity.Product;
import com.example.bowchat.product.service.ProductService;
import com.example.bowchat.user.entity.User;
import com.example.bowchat.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
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
    private final UserService userService;
    private final ProductService productService;
    private final ChatProducer chatProducer;

    @Transactional
    public void placeBid(Long auctionId, Long userId, Long bidAmount) {
        int maxRetries = 3; // 낙관적 락 충돌 시 최대 재시도 횟수

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                doPlaceBid(auctionId, userId, bidAmount);
                return; // 성공 시 반복 종료
            } catch (OptimisticLockingFailureException e) {
                log.warn("동시 입찰 감지 - 경매ID={}, 입찰자ID={}, 재시도 {}/{}", auctionId, userId, attempt, maxRetries);
                try {
                    Thread.sleep(10); // 짧은 대기 (동시 트랜잭션 정리용)
                } catch (InterruptedException ignored) {}
            }
        }

        throw new ResponseStatusException(HttpStatus.CONFLICT, "입찰 충돌이 발생했습니다. 잠시 후 다시 시도해주세요.");
    }

    @Transactional
    protected void doPlaceBid(Long auctionId, Long userId, Long bidAmount) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다."));

        User bidder = userService.findById(userId);

        // 경매 유효성 검증
        auction.validateBid(userId, bidAmount);

        // 실제 입찰 반영
        auction.placeBid(bidder, bidAmount, LocalDateTime.now());
        auctionRepository.save(auction);

        saveBid(bidAmount, auction, bidder);
        log.info("입찰 완료 - 현재가={}, 입찰가={}, 경매ID={}, 입찰자={}", auction.getCurrentPrice(), bidAmount, auctionId, userId);

        // 입찰 이벤트 브로드캐스트
        sendBroadCast(auction.getId(), bidder.getId(), bidder.getEmail(), bidAmount);
        log.info("경매 입찰 성공: auctionId={}, userId={}, bidAmount={}", auctionId, userId, bidAmount);
    }


    private void sendBroadCast(Long auctionId, Long userId, String email, Long bidAmount) {
        chatProducer.send(ChatEventFactory.auctionBid(auctionId, userId, email, bidAmount));
    }


    public List<AuctionResponse> getAllAuctions() {
        return auctionRepository.findAll().stream()
                .map(AuctionResponse::of).toList();
    }

    @Transactional
    public void startAuction(Long productId, LocalDateTime endTime) {
        Product product = productService.getProduct(productId);
        Auction auction = Auction.of(product, endTime);
        auctionRepository.save(auction);
    }

    public AuctionResponse findAuctionById(Long id) {
        Auction auction = auctionRepository.findWithProductAndSellerById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다."));
        return AuctionResponse.of(auction);
    }


    private void saveBid(Long bidAmount, Auction auction, User bidder) {
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
