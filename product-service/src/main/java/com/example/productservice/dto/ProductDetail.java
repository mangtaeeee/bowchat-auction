package com.example.productservice.dto;

import com.example.productservice.entity.Product;
import com.example.productservice.entity.ProductImage;
import com.example.productservice.entity.SaleType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ProductDetail(
        Long productId,
        String productName,
        String productDescription,
        Long productPrice,
        List<String> imageUrls,
        String sellerNickname,
        LocalDateTime createdDate,
        LocalDateTime modifiedDate,
        SaleType saleType,
        boolean isSeller
) {
    public static ProductDetail of(Product product, boolean isSeller, String sellerNickname) {
        return ProductDetail.builder()
                .productId(product.getId())
                .productName(product.getName())
                .productDescription(product.getDescription())
                .productPrice(product.getStartingPrice())
                .imageUrls(product.getProductImages().stream()
                        .map(ProductImage::getUrl)
                        .toList())
                .sellerNickname(sellerNickname)
                .createdDate(product.getCreatedAt())
                .modifiedDate(product.getUpdatedAt())
                .saleType(product.getSaleType())
                .isSeller(isSeller)
                .build();
    }
}
