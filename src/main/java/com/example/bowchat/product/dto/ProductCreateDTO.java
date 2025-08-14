package com.example.bowchat.product.dto;

import com.example.bowchat.product.entity.SaleType;

import java.util.List;

public record ProductCreateDTO(
    String name,
    String description,
    Long price,
    List<String> imageUrls,
    SaleType saleType

) {
}
