package com.example.productservice.service;

import com.example.productservice.dto.ProductCreateDTO;
import com.example.productservice.dto.ProductDetail;
import com.example.productservice.dto.ProductResponse;
import com.example.productservice.entity.Product;
import com.example.productservice.entity.ProductImage;
import com.example.productservice.repository.ProductImageRepository;
import com.example.productservice.repository.ProductRepository;
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
    public Long addProduct(ProductCreateDTO dto) {
        log.info("상품 추가: 이름={}, 설명={}", dto.name(), dto.description());
        Product product = Product.builder()
                .name(dto.name())
                .description(dto.description())
                .startingPrice(dto.price())
                .seller(dto.sellerId())
                .saleType(dto.saleType())
                .build();
        productRepository.save(product);

        List<ProductImage> images = IntStream.range(0, dto.imageUrls().size())
                .mapToObj(i -> {
                    ProductImage image = ProductImage.of(dto.imageUrls().get(i), i, product);
                    product.getProductImages().add(image);
                    return image;
                })
                .toList();
        productImageRepository.saveAll(images);

        return product.getId();
    }

    public ProductDetail getProductDetail(Long productId, Long sellerId, String sellerName) {
        Product product = getProduct(productId);
        boolean isSeller = product.getSeller().equals(sellerId);
        return ProductDetail.of(product, isSeller, sellerName);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAllWithImages().stream()
                .map(ProductResponse::of)
                .toList();
    }

    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }

}
