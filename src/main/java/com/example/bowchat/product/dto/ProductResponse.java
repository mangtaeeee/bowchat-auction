package com.example.bowchat.product.dto;

import com.example.bowchat.product.entity.Product;
import com.example.bowchat.product.entity.ProductImage;

import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String description,
        Long startingPrice,
        String sellerEmail,
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

        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getStartingPrice(),
                p.getSeller().getEmail(),
                thumbnailUrl,
                p.getSaleType().name(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}