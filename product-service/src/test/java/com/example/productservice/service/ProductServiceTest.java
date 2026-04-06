package com.example.productservice.service;

import com.example.productservice.dto.ProductCreateDTO;
import com.example.productservice.dto.ProductDetail;
import com.example.productservice.entity.Product;
import com.example.productservice.entity.ProductImage;
import com.example.productservice.entity.SaleType;
import com.example.productservice.repository.ProductImageRepository;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.user.entity.UserSnapshot;
import com.example.productservice.user.service.UserQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    // 상품 서비스는 저장 시 생성 데이터와 조회 시 응답 조합을 검증한다.
    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private UserQueryService userQueryService;

    @InjectMocks
    private ProductService productService;

    @Test
    void addProductPersistsProductAndImages() {
        // given: 상품 생성 요청과 DB 저장 시 id가 채워지는 상황을 준비한다.
        ProductCreateDTO request = new ProductCreateDTO(
                "Laptop",
                "Gaming laptop",
                1_500_000L,
                List.of("image-1", "image-2"),
                SaleType.AUCTION
        );

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            ReflectionTestUtils.setField(product, "id", 10L);
            return product;
        });

        Long productId = productService.addProduct(request, 7L);

        // then: 저장 결과 id가 반환돼야 한다.
        assertThat(productId).isEqualTo(10L);

        // then: 상품 본문이 seller와 saleType까지 정확히 저장되는지 확인한다.
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getName()).isEqualTo("Laptop");
        assertThat(savedProduct.getSeller()).isEqualTo(7L);
        assertThat(savedProduct.getSaleType()).isEqualTo(SaleType.AUCTION);

        // then: 이미지 목록도 요청 순서대로 저장되는지 확인한다.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductImage>> imageCaptor = ArgumentCaptor.forClass(List.class);
        verify(productImageRepository).saveAll(imageCaptor.capture());
        assertThat(imageCaptor.getValue())
                .extracting(ProductImage::getUrl)
                .containsExactly("image-1", "image-2");
    }

    @Test
    void getProductDetailReturnsSellerFlagAndNickname() {
        // given: 상품, 이미지, 판매자 스냅샷을 직접 구성한다.
        Product product = Product.builder()
                .name("Laptop")
                .description("Gaming laptop")
                .startingPrice(1_500_000L)
                .seller(7L)
                .saleType(SaleType.AUCTION)
                .build();
        ReflectionTestUtils.setField(product, "id", 10L);
        ReflectionTestUtils.setField(product, "createdAt", LocalDateTime.of(2026, 4, 6, 10, 0));
        ReflectionTestUtils.setField(product, "updatedAt", LocalDateTime.of(2026, 4, 6, 11, 0));
        product.getProductImages().add(ProductImage.of("image-1", 0, product));

        UserSnapshot seller = UserSnapshot.builder()
                .userId(7L)
                .email("seller@test.com")
                .nickname("seller")
                .build();

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userQueryService.getUser(7L)).thenReturn(seller);

        ProductDetail detail = productService.getProductDetail(10L, 7L);

        // then: 응답에 판매자 닉네임과 본인 여부가 반영돼야 한다.
        assertThat(detail.productId()).isEqualTo(10L);
        assertThat(detail.sellerNickname()).isEqualTo("seller");
        assertThat(detail.isSeller()).isTrue();
        assertThat(detail.imageUrls()).containsExactly("image-1");
    }
}
