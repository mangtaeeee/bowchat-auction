package com.example.auctionservice.service;

import com.example.auctionservice.client.ProductServiceClient;
import com.example.auctionservice.dto.request.StartAuctionRequest;
import com.example.auctionservice.entity.AuctionErrorCode;
import com.example.auctionservice.entity.AuctionException;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

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
        StartAuctionRequest request = new StartAuctionRequest(10_000L, LocalDateTime.of(2026, 4, 6, 18, 0));
        when(productServiceClient.getSellerId(3L)).thenReturn(7L);

        auctionService.startAuction(3L, 7L, request);

        verify(auctionBidService).createAuction(3L, 7L, request);
    }

    @Test
    void startAuctionRejectsRequesterWhoIsNotSeller() {
        StartAuctionRequest request = new StartAuctionRequest(10_000L, LocalDateTime.of(2026, 4, 6, 18, 0));
        when(productServiceClient.getSellerId(3L)).thenReturn(99L);

        assertThatThrownBy(() -> auctionService.startAuction(3L, 7L, request))
                .isInstanceOf(AuctionException.class)
                .extracting(ex -> ((AuctionException) ex).getErrorCode())
                .isEqualTo(AuctionErrorCode.ONLY_SELLER_CAN_START_AUCTION);

        verifyNoInteractions(auctionBidService);
    }

    @Test
    void placeBidAndBroadcastPublishesAuctionBidEvent() {
        UserSnapshot bidder = UserSnapshot.builder()
                .userId(5L)
                .email("bidder@test.com")
                .nickname("bidder")
                .build();
        when(userQueryService.getUser(5L)).thenReturn(bidder);

        auctionService.placeBidAndBroadcast(9L, 5L, 15_000L);

        verify(auctionBidService).placeBid(9L, 5L, 15_000L);

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
