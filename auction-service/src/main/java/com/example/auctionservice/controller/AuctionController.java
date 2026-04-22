package com.example.auctionservice.controller;

import com.example.auctionservice.auth.UserPrincipal;
import com.example.auctionservice.dto.request.BidRequest;
import com.example.auctionservice.dto.request.StartAuctionRequest;
import com.example.auctionservice.dto.response.AuctionResponse;
import com.example.auctionservice.service.AuctionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Auction", description = "경매 조회와 입찰 API")
public class AuctionController {

    private final AuctionService auctionService;

    @Operation(summary = "경매 시작", description = "상품 판매자가 경매를 시작합니다.")
    @PostMapping("/{productId}/start")
    public ResponseEntity<Void> startAuction(
            @Parameter(description = "경매를 시작할 상품 ID", example = "10")
            @PathVariable Long productId,
            @Valid @RequestBody StartAuctionRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal user
    ) {
        auctionService.startAuction(productId, user.userId(), request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "입찰", description = "입찰에 성공하면 최신 경매 상태를 반환합니다.")
    @PostMapping("/{auctionId}/bid")
    public ResponseEntity<AuctionResponse> placeBid(
            @Parameter(description = "입찰할 경매 ID", example = "1")
            @PathVariable Long auctionId,
            @Valid @RequestBody BidRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal user
    ) {
        log.info("입찰 요청: auctionId={}, bidderId={}, amount={}", auctionId, user.userId(), request.bidAmount());
        return ResponseEntity.ok(
                auctionService.placeBid(auctionId, user.userId(), user.nickname(), request.bidAmount())
        );
    }

    @Operation(summary = "경매 목록 조회")
    @GetMapping
    public ResponseEntity<List<AuctionResponse>> listAuctions() {
        return ResponseEntity.ok(auctionService.getAllAuctions());
    }

    @Operation(summary = "경매 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<AuctionResponse> getAuction(
            @Parameter(description = "조회할 경매 ID", example = "1")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(auctionService.findAuctionById(id));
    }
}
