package com.example.auctionservice.outbox;

import com.example.bowchat.kafkastarter.event.EventMessage;
import com.example.bowchat.kafkastarter.producer.ChatProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelaySchedulerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ChatProducer chatProducer;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    private OutboxRelayProperties properties = relayProperties();

    @InjectMocks
    private OutboxRelayScheduler outboxRelayScheduler;

    @Test
    void relayPendingEventsPublishesClaimedAuctionBidEvent() throws Exception {
        OutboxEvent event = OutboxEvent.pending(
                AuctionOutboxService.AUCTION_AGGREGATE,
                9L,
                AuctionOutboxService.AUCTION_BID_PLACED,
                objectMapper.writeValueAsString(new AuctionBidPlacedPayload(9L, 5L, "bidder", 15_000L, 1234L)),
                LocalDateTime.now()
        );

        when(outboxEventRepository.recoverStuckEvents(any(), any(), any(), any(), any())).thenReturn(0);
        when(outboxEventRepository.findProcessableIds(eq(OutboxEventStatus.PENDING), any(), any())).thenReturn(List.of(event.getId()));
        when(outboxEventRepository.claim(eq(event.getId()), any(), any(), any(), any())).thenReturn(1);
        when(outboxEventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        outboxRelayScheduler.relayPendingEvents();

        ArgumentCaptor<EventMessage> messageCaptor = ArgumentCaptor.forClass(EventMessage.class);
        verify(chatProducer).send(messageCaptor.capture());
        EventMessage message = messageCaptor.getValue();
        assertThat(message.roomId()).isEqualTo(9L);
        assertThat(message.senderId()).isEqualTo(5L);
        assertThat(message.senderName()).isEqualTo("bidder");
        assertThat(message.content()).isEqualTo("15000");
        verify(outboxEventRepository).markPublished(eq(event.getId()), eq(OutboxEventStatus.PROCESSING), eq(OutboxEventStatus.PUBLISHED), any());
    }

    @Test
    void relayPendingEventsReschedulesWhenPublishFails() throws Exception {
        OutboxEvent event = OutboxEvent.pending(
                AuctionOutboxService.AUCTION_AGGREGATE,
                9L,
                AuctionOutboxService.AUCTION_BID_PLACED,
                objectMapper.writeValueAsString(new AuctionBidPlacedPayload(9L, 5L, "bidder", 15_000L, 1234L)),
                LocalDateTime.now()
        );

        when(outboxEventRepository.recoverStuckEvents(any(), any(), any(), any(), any())).thenReturn(0);
        when(outboxEventRepository.findProcessableIds(eq(OutboxEventStatus.PENDING), any(), any())).thenReturn(List.of(event.getId()));
        when(outboxEventRepository.claim(eq(event.getId()), any(), any(), any(), any())).thenReturn(1);
        when(outboxEventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        doThrow(new IllegalStateException("broker down")).when(chatProducer).send(any(EventMessage.class));

        outboxRelayScheduler.relayPendingEvents();

        verify(outboxEventRepository).reschedule(eq(event.getId()), eq(OutboxEventStatus.PROCESSING), eq(OutboxEventStatus.PENDING), any(), eq("broker down"));
    }

    private static OutboxRelayProperties relayProperties() {
        OutboxRelayProperties properties = new OutboxRelayProperties();
        properties.setBatchSize(10);
        properties.setRetryDelaySeconds(5);
        properties.setStaleThresholdSeconds(30);
        return properties;
    }
}
