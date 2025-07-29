package com.example.bowchat.product.controller;

import com.example.bowchat.product.dto.ProductCreateDTO;
import com.example.bowchat.product.dto.ProductResponse;
import com.example.bowchat.product.service.ProductService;
import com.example.bowchat.user.entity.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> listProducts() {
        List<ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @PostMapping
    public ResponseEntity<Long> addProduct(
            @RequestBody ProductCreateDTO productCreateDTO,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        log.info("상품 추가 요청: {}", productCreateDTO);

        Long l = productService.addProduct(productCreateDTO, principalDetails.getUser());
        log.info("상품 추가 성공: ID={}", l);

        return ResponseEntity.ok(l);
    }


}
