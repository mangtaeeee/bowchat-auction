package com.example.bowchat.product.dto;

import com.example.bowchat.product.entity.Product;

import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String description,
        Long startingPrice,
        String sellerEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductResponse of(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getStartingPrice(),
                p.getSeller().getEmail(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}