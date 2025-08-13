package com.example.bowchat.product.service;

import com.example.bowchat.product.dto.ProductCreateDTO;
import com.example.bowchat.product.entity.Product;
import com.example.bowchat.product.entity.SaleType;
import com.example.bowchat.product.repository.ProductRepository;
import com.example.bowchat.user.entity.ProviderType;
import com.example.bowchat.user.entity.Role;
import com.example.bowchat.user.entity.User;
import com.example.bowchat.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest(
        // 임베디드 Kafka 주소를 spring.kafka.bootstrap-servers 에 강제로 주입
        properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
)
@EmbeddedKafka(partitions = 1, topics = "auction-bid")
@ActiveProfiles("dev")
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private User seller;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        userRepository.deleteAll();

        seller = User.builder()
                .email("test@naver.com")
                .nickname("홍길동")
                .provider(ProviderType.LOCAL)
                .role(Role.USER)
                .build();
        userRepository.save(seller);
    }

    @Test
    @DisplayName("상품 등록 시 DB에 저장된다")
    @Transactional
    void addProduct_success() {
        // given
        ProductCreateDTO request = new ProductCreateDTO(
                "아이폰15",
                "미개봉입니다",
                1200000L,
                List.of("http://img1.jpg", "http://img2.jpg"),
                SaleType.DIRECT
        );

        // when
        productService.addProduct(request, seller);

        // then
        List<Product> products = productRepository.findAllWithImages();

    }
}
