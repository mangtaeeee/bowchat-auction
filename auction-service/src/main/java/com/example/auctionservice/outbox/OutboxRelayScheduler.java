package com.example.auctionservice.outbox;

import com.example.bowchat.kafkastarter.event.EventMessage;
import com.example.bowchat.kafkastarter.event.MessageType;
import com.example.bowchat.kafkastarter.producer.ChatProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayScheduler {

    private static final int LAST_ERROR_MAX_LENGTH = 500;

    private final OutboxEventRepository outboxEventRepository;
    private final ChatProducer chatProducer;
    private final ObjectMapper objectMapper;
    private final OutboxRelayProperties properties;

    @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay-ms:2000}")
    public void relayPendingEvents() {
        LocalDateTime now = LocalDateTime.now();
        recoverStuckEvents(now);

        List<String> eventIds = outboxEventRepository.findProcessableIds(
                OutboxEventStatus.PENDING,
                now,
                PageRequest.of(0, properties.getBatchSize())
        );

        for (String eventId : eventIds) {
            if (!claim(eventId, now)) {
                continue;
            }
            publishClaimedEvent(eventId);
        }
    }

    private void recoverStuckEvents(LocalDateTime now) {
        int recovered = outboxEventRepository.recoverStuckEvents(
                OutboxEventStatus.PROCESSING,
                OutboxEventStatus.PENDING,
                now.minusSeconds(properties.getStaleThresholdSeconds()),
                now,
                "처리가 멈춰 있던 Outbox 이벤트를 다시 재시도할 수 있게 되돌렸습니다."
        );

        if (recovered > 0) {
            log.warn("처리가 멈춰 있던 Outbox 이벤트 {}건을 다시 재시도 대상으로 돌렸습니다.", recovered);
        }
    }

    private boolean claim(String eventId, LocalDateTime now) {
        return outboxEventRepository.claim(
                eventId,
                OutboxEventStatus.PENDING,
                OutboxEventStatus.PROCESSING,
                now,
                now
        ) == 1;
    }

    void publishClaimedEvent(String eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("처리할 Outbox 이벤트를 찾지 못했습니다. eventId=" + eventId));

        try {
            chatProducer.send(toChatEvent(event));
            outboxEventRepository.markPublished(
                    eventId,
                    OutboxEventStatus.PROCESSING,
                    OutboxEventStatus.PUBLISHED,
                    LocalDateTime.now()
            );
            log.info("Outbox 이벤트 발행을 마쳤습니다. eventId={}, eventType={}", eventId, event.getEventType());
        } catch (Exception e) {
            LocalDateTime nextAttemptAt = LocalDateTime.now().plusSeconds(properties.getRetryDelaySeconds());
            outboxEventRepository.reschedule(
                    eventId,
                    OutboxEventStatus.PROCESSING,
                    OutboxEventStatus.PENDING,
                    nextAttemptAt,
                    abbreviateError(e)
            );
            log.error("Outbox 이벤트를 보내는 중 문제가 생겼습니다. eventId={}, eventType={}", eventId, event.getEventType(), e);
        }
    }

    private EventMessage toChatEvent(OutboxEvent event) throws JsonProcessingException {
        if (!AuctionOutboxService.AUCTION_BID_PLACED.equals(event.getEventType())) {
            throw new IllegalStateException("이 Outbox 이벤트 타입은 아직 처리할 수 없습니다: " + event.getEventType());
        }

        AuctionBidPlacedPayload payload = objectMapper.readValue(event.getPayload(), AuctionBidPlacedPayload.class);
        return EventMessage.builder()
                .roomId(payload.auctionId())
                .senderId(payload.bidderId())
                .senderName(payload.bidderNickname())
                .topicName(MessageType.AUCTION_BID.getTopicName())
                .messageType(MessageType.AUCTION_BID.name())
                .content(String.valueOf(payload.bidAmount()))
                .timestamp(payload.occurredAt())
                .build();
    }

    private String abbreviateError(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = e.getClass().getSimpleName();
        }
        if (message.length() <= LAST_ERROR_MAX_LENGTH) {
            return message;
        }
        return message.substring(0, LAST_ERROR_MAX_LENGTH);
    }
}
