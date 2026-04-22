package com.example.auctionservice.controller;

import com.example.auctionservice.dto.response.AuctionResponse;
import com.example.auctionservice.service.AuctionService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/auctions")
@RequiredArgsConstructor
@Hidden
public class InternalAuctionController {

    private final AuctionService auctionService;

    @GetMapping("/{auctionId}")
    public ResponseEntity<AuctionResponse> getAuction(@PathVariable Long auctionId) {
        return ResponseEntity.ok(auctionService.findAuctionById(auctionId));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<AuctionResponse> getAuctionByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(auctionService.findAuctionByProductId(productId));
    }
}
