package com.example.auctionservice.service;

import com.example.auctionservice.client.ProductServiceClient;
import com.example.auctionservice.dto.request.StartAuctionRequest;
import com.example.auctionservice.user.entity.UserSnapshot;
import com.example.auctionservice.user.service.UserQueryService;
import com.example.bowchat.kafkastarter.event.EventMessage;
import com.example.bowchat.kafkastarter.event.MessageType;
import com.example.bowchat.kafkastarter.producer.ChatProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    // 경매 서비스는 seller 권한 검증과 입찰 브로드캐스트가 핵심이다.
    @Mock
    private AuctionBidService auctionBidService;

    @Mock
    private com.example.auctionservice.repository.AuctionRepository auctionRepository;

    @Mock
    private ChatProducer chatProducer;

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private ProductServiceClient productServiceClient;

    @InjectMocks
    private AuctionService auctionService;

    @Test
    void startAuctionCreatesAuctionWhenRequesterIsSeller() {
        // given: 상품 판매자와 요청 사용자가 같은 상황을 만든다.
        StartAuctionRequest request = new StartAuctionRequest(10_000L, LocalDateTime.of(2026, 4, 6, 18, 0));
        when(productServiceClient.getSellerId(3L)).thenReturn(7L);

        auctionService.startAuction(3L, 7L, request);

        // then: 경매 생성 위임이 호출돼야 한다.
        verify(auctionBidService).createAuction(3L, 7L, request);
    }

    @Test
    void startAuctionRejectsRequesterWhoIsNotSeller() {
        // given: 상품 판매자와 요청 사용자가 다른 상황을 만든다.
        StartAuctionRequest request = new StartAuctionRequest(10_000L, LocalDateTime.of(2026, 4, 6, 18, 0));
        when(productServiceClient.getSellerId(3L)).thenReturn(99L);

        // when/then: 판매자가 아니면 403을 던지고 생성은 일어나면 안 된다.
        assertThatThrownBy(() -> auctionService.startAuction(3L, 7L, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        verifyNoInteractions(auctionBidService);
    }

    @Test
    void placeBidAndBroadcastPublishesAuctionBidEvent() {
        // given: 입찰자 닉네임을 조회할 수 있는 상황을 만든다.
        UserSnapshot bidder = UserSnapshot.builder()
                .userId(5L)
                .email("bidder@test.com")
                .nickname("bidder")
                .build();
        when(userQueryService.getUser(5L)).thenReturn(bidder);

        auctionService.placeBidAndBroadcast(9L, 5L, 15_000L);

        // then: 실제 입찰 로직과 Kafka 브로드캐스트가 둘 다 실행돼야 한다.
        verify(auctionBidService).placeBid(9L, 5L, 15_000L);

        // 브로드캐스트 payload가 auction-bid 이벤트 규격을 따르는지 확인한다.
        ArgumentCaptor<EventMessage> eventCaptor = ArgumentCaptor.forClass(EventMessage.class);
        verify(chatProducer).send(eventCaptor.capture());
        EventMessage event = eventCaptor.getValue();
        assertThat(event.roomId()).isEqualTo(9L);
        assertThat(event.senderId()).isEqualTo(5L);
        assertThat(event.senderName()).isEqualTo("bidder");
        assertThat(event.topicName()).isEqualTo(MessageType.AUCTION_BID.getTopicName());
        assertThat(event.messageType()).isEqualTo(MessageType.AUCTION_BID.name());
        assertThat(event.content()).isEqualTo("15000");
    }
}
