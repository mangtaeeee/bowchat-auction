package com.example.bowchat.auction.controller;

import com.example.bowchat.auction.dto.AuctionResponse;
import com.example.bowchat.auction.dto.StartAuctionRequest;
import com.example.bowchat.auction.service.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    /** 1) 경매 시작 (상품 ID + 종료시간) */
    @PostMapping("/{productId}/start")
    public ResponseEntity<Void> startAuction(
            @PathVariable Long productId,
            @RequestBody StartAuctionRequest request
    ) {
        auctionService.startAuction(productId, request.endTime());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<AuctionResponse>> listAuctions() {
        List<AuctionResponse> list = auctionService.getAllAuctions();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuctionResponse> getAuction(@PathVariable Long id) {
        return ResponseEntity.ok(auctionService.findAuctionById(id));
    }
}