package com.example.auctionservice.service;

import com.example.auctionservice.client.ProductServiceClient;
import com.example.auctionservice.dto.request.StartAuctionRequest;
import com.example.auctionservice.dto.response.AuctionResponse;
import com.example.auctionservice.entity.AuctionErrorCode;
import com.example.auctionservice.entity.AuctionException;
import com.example.auctionservice.repository.AuctionRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionService {

    private final AuctionBidService auctionBidService;
    private final AuctionRepository auctionRepository;
    private final ProductServiceClient productServiceClient;

    public AuctionResponse placeBid(Long auctionId, Long bidderId, String bidderNickname, Long bidAmount) {
        return AuctionResponse.of(
                auctionBidService.placeBid(auctionId, bidderId, bidderNickname, bidAmount)
        );
    }

    public void startAuction(Long productId, Long requestUserId, StartAuctionRequest request) {
        Long sellerId = resolveSellerId(productId);
        validateSellerPermission(sellerId, requestUserId);

        auctionBidService.createAuction(productId, sellerId, request);
        log.info("경매 시작 요청 처리: productId={}, sellerId={}", productId, sellerId);
    }

    @Transactional(readOnly = true)
    public AuctionResponse findAuctionByProductId(Long productId) {
        return AuctionResponse.of(auctionRepository.findByProduct(productId)
                .orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND_BY_PRODUCT)));
    }

    @Transactional(readOnly = true)
    public List<AuctionResponse> getAllAuctions() {
        return auctionRepository.findAll().stream()
                .map(AuctionResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public AuctionResponse findAuctionById(Long id) {
        return AuctionResponse.of(auctionRepository.findById(id)
                .orElseThrow(() -> new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND)));
    }

    private Long resolveSellerId(Long productId) {
        try {
            return productServiceClient.getSellerId(productId);
        } catch (FeignException.NotFound e) {
            throw new AuctionException(AuctionErrorCode.PRODUCT_NOT_FOUND);
        } catch (FeignException e) {
            log.error("상품 서비스 호출 실패: productId={}, status={}", productId, e.status(), e);
            throw new AuctionException(AuctionErrorCode.PRODUCT_SERVICE_UNAVAILABLE);
        }
    }

    private void validateSellerPermission(Long sellerId, Long requestUserId) {
        if (!sellerId.equals(requestUserId)) {
            throw new AuctionException(AuctionErrorCode.ONLY_SELLER_CAN_START_AUCTION);
        }
    }
}
