package com.example.productservice.controller;

import com.example.productservice.dto.ProductInfo;
import com.example.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내부 API 컨트롤러로, 다른 서비스에서 호출하는 API를 정의합니다.
 */
@RestController
@RequestMapping("/internal/products")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductService productService;


    // auction-service에서 판매자 확인용
    @GetMapping("/{productId}/seller")
    public ResponseEntity<Long> getSellerId(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getSellerIdByProductId(productId));
    }

    // chat-service에서 상품 정보 조회용
    @GetMapping("/{productId}")
    public ResponseEntity<ProductInfo> getProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProductInfo(productId));
    }
}
