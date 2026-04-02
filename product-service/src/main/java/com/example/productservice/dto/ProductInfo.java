package com.example.productservice.dto;

public record ProductInfo(
        Long id,
        String name,
        Long sellerId
) {
}
