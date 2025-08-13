package com.example.bowchat.product.service;

import com.example.bowchat.product.dto.ProductCreateDTO;
import com.example.bowchat.product.dto.ProductResponse;
import com.example.bowchat.product.entity.Product;
import com.example.bowchat.product.entity.ProductImage;
import com.example.bowchat.product.repository.ProductImageRepository;
import com.example.bowchat.product.repository.ProductRepository;
import com.example.bowchat.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductService {


    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    @Transactional
    public Long addProduct(ProductCreateDTO dto, User seller) {
        log.info("상품 추가: 이름={}, 설명={}", dto.name(), dto.description());
        Product product = Product.builder()
                .name(dto.name())
                .description(dto.description())
                .startingPrice(dto.price())
                .seller(seller)
                .saleType(dto.saleType())
                .build();
        productRepository.save(product);

        List<ProductImage> images = IntStream.range(0, dto.imageUrl().size())
                .mapToObj(i -> {
                    ProductImage image = ProductImage.of(dto.imageUrl().get(i), i, product);
                    product.getProductImages().add(image);
                    return image;
                })
                .toList();
        productImageRepository.saveAll(images);

        return product.getId();
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponse::of)
                .toList();
    }

    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }

}
