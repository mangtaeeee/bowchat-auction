package com.example.productservice.controller;

import com.example.productservice.dto.ProductCreateDTO;
import com.example.productservice.dto.ProductDetail;
import com.example.productservice.dto.ProductResponse;
import com.example.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> listProducts() {
        List<ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetail> getProductDetail(
            @PathVariable Long productId,
            @RequestParam Long userId,
            String sellerName) {

        ProductDetail productDetail = productService.getProductDetail(productId, userId, sellerName);
        return ResponseEntity.ok(productDetail);
    }

    @PostMapping
    public ResponseEntity<Long> addProduct(
            @RequestBody ProductCreateDTO productCreateDTO) {
        log.info("상품 추가 요청: {}", productCreateDTO);

        Long l = productService.addProduct(productCreateDTO);
        log.info("상품 추가 성공: ID={}", l);

        return ResponseEntity.ok(l);
    }


}
