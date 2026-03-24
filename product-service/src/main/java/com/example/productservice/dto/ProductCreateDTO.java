package com.example.productservice.dto;


import com.example.productservice.entity.SaleType;

import java.util.List;

public record ProductCreateDTO(
    String name,
    String description,
    Long price,
    List<String> imageUrls,
    SaleType saleType,
    Long sellerId

) {
}
