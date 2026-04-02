package com.example.auctionservice.controller;


import com.example.auctionservice.auth.UserPrincipal;
import com.example.auctionservice.dto.request.BidRequest;
import com.example.auctionservice.dto.request.StartAuctionRequest;
import com.example.auctionservice.dto.response.AuctionResponse;
import com.example.auctionservice.service.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
@Slf4j
public class AuctionController {

    private final AuctionService auctionService;

    // 경매 시작 - sellerId는 JWT에서 추출
    @PostMapping("/{productId}/start")
    public ResponseEntity<Void> startAuction(
            @PathVariable Long productId,
            @RequestBody StartAuctionRequest request,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        auctionService.startAuction(productId, user.userId(), request);
        return ResponseEntity.ok().build();
    }

    // 입찰 - bidderId는 JWT에서 추출
    @PostMapping("/{auctionId}/bid")
    public ResponseEntity<Void> placeBid(
            @PathVariable Long auctionId,
            @RequestBody BidRequest request,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        log.info("입찰 요청: auctionId={}, bidderId={}, amount={}", auctionId, user.userId(), request.bidAmount());
        auctionService.placeBidAndBroadcast(auctionId, user.userId(), request.bidAmount());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<AuctionResponse>> listAuctions() {
        return ResponseEntity.ok(auctionService.getAllAuctions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuctionResponse> getAuction(@PathVariable Long id) {
        return ResponseEntity.ok(auctionService.findAuctionById(id));
    }
}