package com.example.productservice.dto;

import com.example.productservice.entity.Product;
import com.example.productservice.entity.ProductImage;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ProductResponse(
        Long id,
        String name,
        String description,
        Long startingPrice,
        String thumbnailUrl,
        String saleType, // 예: "AUCTION", "DIRECT"
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductResponse of(Product p) {
        String thumbnailUrl = p.getProductImages().stream()
                .filter(ProductImage::isThumbnail)
                .findFirst()
                .map(ProductImage::getUrl)
                .orElse(null);

        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .startingPrice(p.getStartingPrice())
                .thumbnailUrl(thumbnailUrl)
                .saleType(p.getSaleType().name())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}