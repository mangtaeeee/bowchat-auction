package com.example.bowchat.product.dto;

public record ProductCreateDTO(
    String name,
    String description,
    Long price,
    String imageUrl,
    Long categoryId,
    Long sellerId
) {
}
