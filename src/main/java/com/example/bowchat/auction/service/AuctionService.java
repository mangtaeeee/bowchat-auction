package com.example.bowchat.auction.service;

import com.example.bowchat.auction.dto.AuctionResponse;
import com.example.bowchat.auction.entity.Auction;
import com.example.bowchat.auction.entity.AuctionBid;
import com.example.bowchat.auction.repository.AuctionBidRepository;
import com.example.bowchat.auction.repository.AuctionRepository;
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

    @Transactional
    public void placeBid(Long auctionId, Long userId, Long bidAmount) {

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow( ()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다."));
        User bidder = userService.findById(userId);
        if (auction.getProduct().getSeller().getId().equals(bidder.getId())) {
            log.warn("판매자는 자신의 상품에 입찰할 수 없습니다. userId={}", userId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "판매자는 자신의 상품에 입찰할 수 없습니다.");
        }

        if (bidAmount <= auction.getCurrentPrice()) {
            log.warn("입찰 금액이 현재가보다 낮거나 같습니다. current={}, bid={}", auction.getCurrentPrice(), bidAmount);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "입찰 금액이 현재가보다 낮거나 같습니다.");
        }


        auction.placeBid(bidder, bidAmount, LocalDateTime.now());
        auctionRepository.save(auction);
        savedBid(bidAmount, auction, bidder);
        log.info("경매 입찰 성공: 경매 ID={}, 입찰자 ID={}, 입찰 금액={}", auctionId, userId, bidAmount);
    }

    public List<AuctionResponse> getAllAuctions() {
        return auctionRepository.findAll().stream()
                .map(a -> AuctionResponse.builder()
                        .id(a.getId())
                        .productId(a.getProduct().getId())
                        .productName(a.getProduct().getName())
                        .currentPrice(a.getCurrentPrice())
                        .endTime(a.getEndTime())
                        .build()
                ).toList();
    }

    @Transactional
    public void startAuction(Long productId, LocalDateTime endTime) {
        Product product = productService.getProduct(productId);
        Auction auction = Auction.of(product, endTime);
        auctionRepository.save(auction);
    }


    private void savedBid(Long bidAmount, Auction auction, User bidder) {
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
