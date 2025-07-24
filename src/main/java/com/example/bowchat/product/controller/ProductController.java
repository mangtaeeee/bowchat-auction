package com.example.bowchat.product.controller;

import com.example.bowchat.product.dto.ProductCreateDTO;
import com.example.bowchat.product.service.ProductService;
import com.example.bowchat.user.entity.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<Void> addProduct(
            @RequestBody ProductCreateDTO productCreateDTO,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        log.info("상품 추가 요청: {}", productCreateDTO);
        productService.addProduct(productCreateDTO, principalDetails.getUser());
        return ResponseEntity.ok().build();
    }


}
