package com.example.bowchat.auction.service;

import com.example.bowchat.auction.entity.Auction;
import com.example.bowchat.auction.entity.AuctionBid;
import com.example.bowchat.auction.repository.AuctionBidRepository;
import com.example.bowchat.auction.repository.AuctionRepository;
import com.example.bowchat.user.entity.User;
import com.example.bowchat.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final AuctionBidRepository auctionBidRepository;
    private final UserService userService;

    @Transactional
    public void placeBid(Long auctionId, Long userId, Long bidAmount) {

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow( ()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다."));
        User bidder = userService.findById(userId);
        auction.placeBid(bidder, bidAmount, LocalDateTime.now());
        auctionRepository.save(auction);
        savedBid(bidAmount, auction, bidder);
        log.info("경매 입찰 성공: 경매 ID={}, 입찰자 ID={}, 입찰 금액={}", auctionId, userId, bidAmount);
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
