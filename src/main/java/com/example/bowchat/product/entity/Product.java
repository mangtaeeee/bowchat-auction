package com.example.bowchat.product.entity;

import com.example.bowchat.chatroom.entity.ChatRoom;
import com.example.bowchat.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PRODUCTS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRODUCT_ID")
    private Long id;

    private String name;
    private String description;

    @Column(name = "STARTING_PRICE")
    private Long startingPrice;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SELLER_ID")
    private User seller;

//    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
//    private Auction auction;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatRoom> chatRooms = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "PRODUCT_IMAGES",
            joinColumns = @JoinColumn(name = "PRODUCT_ID")
    )
    @Column(name = "IMAGE_URL")
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    // 생성 시점 자동 설정
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    // 수정 시점 자동 업데이트
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateInfo(String name, String description, Long startingPrice) {
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
    }
}
