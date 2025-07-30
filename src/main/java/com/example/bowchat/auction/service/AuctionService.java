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

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow( ()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다."));
        User bidder = userService.findById(userId);

        validateBidderIsNotSeller(auction, bidder);
        validateBidAmountIsHigher(bidAmount, auction);

        auction.placeBid(bidder, bidAmount, LocalDateTime.now());
        auctionRepository.save(auction);
        saveBid(bidAmount, auction, bidder);
        log.info("현재가={}, 입찰가={}", auction.getCurrentPrice(), bidAmount);
        sendBroadCast(auction.getId(), bidder.getId(), bidder.getEmail(), bidAmount);

        log.info("경매 입찰 성공: 경매 ID={}, 입찰자 ID={}, 입찰 금액={}", auctionId, userId, bidAmount);
    }

    private void sendBroadCast(Long auctionId, Long userId, String email, Long bidAmount) {
        chatProducer.send(ChatEventFactory.auctionBid(auctionId, userId, email, bidAmount));
    }

    private static void validateBidAmountIsHigher(Long bidAmount, Auction auction) {
        if (bidAmount <= auction.getCurrentPrice()) {
            log.warn("입찰 금액이 현재가보다 낮거나 같습니다. current={}, bid={}", auction.getCurrentPrice(), bidAmount);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "입찰 금액이 현재가보다 낮거나 같습니다.");
        }
    }

    private static void validateBidderIsNotSeller(Auction auction, User bidder) {
        if (auction.getProduct().getSeller().getId().equals(bidder.getId())) {
            log.warn("판매자는 자신의 상품에 입찰할 수 없습니다. userId={}", bidder.getId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "판매자는 자신의 상품에 입찰할 수 없습니다.");
        }
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
