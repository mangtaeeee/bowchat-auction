package com.example.productservice.service;

import com.example.productservice.dto.ProductCreateDTO;
import com.example.productservice.dto.ProductDetail;
import com.example.productservice.dto.ProductInfo;
import com.example.productservice.dto.ProductResponse;
import com.example.productservice.entity.Product;
import com.example.productservice.entity.ProductImage;
import com.example.productservice.repository.ProductImageRepository;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.user.entity.UserSnapshot;
import com.example.productservice.user.service.UserQueryService;
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
    private final UserQueryService userQueryService;

    @Transactional
    public Long addProduct(ProductCreateDTO dto, Long sellerId) {
        log.info("상품 추가: 이름={}, sellerId={}", dto.name(), sellerId);
        Product product = Product.builder()
                .name(dto.name())
                .description(dto.description())
                .startingPrice(dto.price())
                .seller(sellerId)
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

    public ProductDetail getProductDetail(Long productId, Long requestUserId) {
        Product product = getProduct(productId);

        // user-service 호출 없이 로컬 UserSnapshot → Redis → HTTP fallback 순으로 조회
        UserSnapshot seller = userQueryService.getUser(product.getSeller());

        boolean isSeller = product.getSeller().equals(requestUserId);
        return ProductDetail.of(product, isSeller, seller.getNickname());
    }

    public ProductInfo getProductInfo(Long productId) {
        Product product = getProduct(productId); // 없으면 404 자동
        return new ProductInfo(product.getId(), product.getName(), product.getSeller());
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

    public Long getSellerIdByProductId(Long productId) {
        return getProduct(productId).getSeller();
    }

}
