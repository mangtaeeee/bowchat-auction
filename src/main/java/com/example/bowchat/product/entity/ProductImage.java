package com.example.bowchat.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Entity
@Table(name = "PRODUCT_IMAGES")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(nullable = false)
    private int position; // 0이 썸네일

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID")
    private Product product;

    public static ProductImage of(String url, int position, Product product) {

        if (position < 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "이미지를 등록해 주세요.");
        }
        return ProductImage.builder()
                .url(url)
                .position(position)
                .product(product)
                .build();
    }

    // 썸네일 여부
    public boolean isThumbnail() { return position == 0; }

    public void setProduct(Product product) {
        if (this.product != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 다른 상품에 등록된 이미지입니다.");
        }
        this.product = product;
        product.getProductImages().add(this); // 양방향 연관관계 설정
    }

}
