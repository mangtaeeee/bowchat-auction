package com.example.bowchat.product.dto;

import com.example.bowchat.product.entity.Product;
import com.example.bowchat.product.entity.ProductImage;
import com.example.bowchat.product.entity.SaleType;
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
    public static ProductDetail of(Product product, boolean isSeller) {
        return ProductDetail.builder()
                .productId(product.getId())
                .productName(product.getName())
                .productDescription(product.getDescription())
                .productPrice(product.getStartingPrice())
                .imageUrls(product.getProductImages().stream()
                        .map(ProductImage::getUrl)
                        .toList())
                .sellerNickname(product.getSeller().getNickname())
                .createdDate(product.getCreatedAt())
                .modifiedDate(product.getUpdatedAt())
                .saleType(product.getSaleType())
                .isSeller(isSeller)
                .build();
    }
}
