package com.example.bowchat.kafka;

import com.example.bowchat.auction.entity.Auction;
import com.example.bowchat.auction.entity.AuctionBid;
import com.example.bowchat.auction.repository.AuctionBidRepository;
import com.example.bowchat.auction.repository.AuctionRepository;
import com.example.bowchat.chatmessage.entity.MessageType;
import com.example.bowchat.product.dto.ProductCreateDTO;
import com.example.bowchat.product.entity.Product;
import com.example.bowchat.product.repository.ProductRepository;
import com.example.bowchat.product.service.ProductService;
import com.example.bowchat.user.dto.SingUpRequest;
import com.example.bowchat.user.entity.User;
import com.example.bowchat.user.repository.UserRepository;
import com.example.bowchat.user.service.UserService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest(
        // 임베디드 Kafka 주소를 spring.kafka.bootstrap-servers 에 강제로 주입
        properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
)
@EmbeddedKafka(partitions = 1, topics = "auction-bid")
@ActiveProfiles("dev")
class AuctionBidConsumerTest {

    @Autowired KafkaTemplate<String, ChatEvent> kafkaTemplate;

    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired ProductService productService;
    @Autowired ProductRepository productRepository;
    @Autowired AuctionRepository auctionRepository;
    @Autowired AuctionBidRepository bidRepository;

    private Auction auction;
    private User bidder;

    @BeforeEach
    void setUp() {
        // DB 초기화
        bidRepository.deleteAll();
        auctionRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        // 테스트 유저/상품/경매 준비
        userService.signup(new SingUpRequest("test@example.com", "pass", "테스터"));
        bidder = userRepository.findByEmail("test@example.com").orElseThrow();

        productService.addProduct(
                new ProductCreateDTO("테스트상품", "설명", 1000L, "http://img", 1L, bidder.getId()),
                bidder
        );
        Product product = productRepository.findAll().get(0);

        auction = auctionRepository.save(
                Auction.of(product, LocalDateTime.now().plusMinutes(5))
        );
    }

    @Test
    void bidEvents_shouldBePersistedThroughConsumer_andMeasureLatency() {
        int bidCount = 5;
        // 1) 발행 직전 타임스탬프 기록
        long publishStart = System.currentTimeMillis();

        for (int i = 1; i <= bidCount; i++) {
            kafkaTemplate.send("auction-bid", ChatEvent.builder()
                    .roomId(auction.getId())
                    .senderId(bidder.getId())
                    .senderName(bidder.getEmail())
                    .type(MessageType.AUCTION_BID)
                    .content(String.valueOf(1000 + i * 100))
                    .timestamp(System.currentTimeMillis())
                    .build()
            );
        }

        // 2) DB 반영을 기다리며, 반영 성공 시점의 타임스탬프를 캡처
        final long[] persistTime = {0};
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    List<AuctionBid> bids = bidRepository.findByAuction_IdOrderByBidTimeAsc(auction.getId());
                    assertThat(bids).hasSize(bidCount);
                    // 첫/마지막 금액 검증
                    assertThat(bids.get(0).getAmount()).isEqualTo(1100L);
                    assertThat(bids.get(bids.size() - 1).getAmount()).isEqualTo(1000 + bidCount * 100L);

                    // 여기까지 오면 DB 반영 완료
                    persistTime[0] = System.currentTimeMillis();
                });

        // 3) 레이턴시 로깅
        long latencyMs = persistTime[0] - publishStart;
        System.out.println("DB 반영 레이턴시: " + latencyMs + " ms");
    }
}