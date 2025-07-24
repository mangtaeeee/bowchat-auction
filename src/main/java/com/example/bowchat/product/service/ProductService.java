package com.example.bowchat.product.service;

import com.example.bowchat.product.dto.ProductCreateDTO;
import com.example.bowchat.product.entity.Product;
import com.example.bowchat.product.repository.productRepository;
import com.example.bowchat.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductService {


    private final productRepository productRepository;

    @Transactional
    public void addProduct(ProductCreateDTO dto,  User seller) {
        log.info("상품 추가: 이름={}, 설명={}", dto.name(), dto.description());
        Product product = Product.builder()
                .name(dto.name())
                .description(dto.description())
                .startingPrice(dto.price())
                .seller(seller)
                .build();
        productRepository.save(product);
    }

    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }

}
