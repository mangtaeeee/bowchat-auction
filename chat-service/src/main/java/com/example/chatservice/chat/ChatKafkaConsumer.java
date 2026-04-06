package com.example.chatservice.chat;

import com.example.bowchat.kafkastarter.event.EventMessage;
import com.example.chatservice.chatmessage.service.ChatMessageService;
import com.example.chatservice.websocket.RedisChatBroadcastPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatKafkaConsumer {

    private final ChatMessageService chatMessageService;
    private final RedisChatBroadcastPublisher redisChatBroadcastPublisher;
    private final ObjectMapper objectMapper;

    // 일반 채팅 메시지 수신 → MongoDB 저장 + WebSocket 브로드캐스트
    @KafkaListener(topics = "chat-message", groupId = "chat-service-group")
    public void handleChatMessage(String message) {
        try {
            EventMessage event = objectMapper.readValue(message, EventMessage.class);
            log.info("chat-message 수신: roomId={}, senderId={}", event.roomId(), event.senderId());

            // MongoDB 저장
            chatMessageService.save(event);

            // 모든 chat-service 인스턴스에 fan-out
            String payload = objectMapper.writeValueAsString(event);
            redisChatBroadcastPublisher.publish(payload);

        } catch (Exception e) {
            log.error("chat-message 처리 실패: {}", message, e);
            throw new RuntimeException(e);
        }
    }

    // 경매 입찰 이벤트 수신 → MongoDB 저장 + 경매방 WebSocket 브로드캐스트
    @KafkaListener(topics = "auction-bid", groupId = "chat-service-group")
    public void handleAuctionBid(String message) {
        try {
            EventMessage event = objectMapper.readValue(message, EventMessage.class);
            log.info("auction-bid 수신: auctionId={}, bidderId={}, amount={}",
                    event.roomId(), event.senderId(), event.content());

            // MongoDB 저장
            chatMessageService.save(event);

            // 모든 chat-service 인스턴스에 fan-out
            String payload = objectMapper.writeValueAsString(event);
            redisChatBroadcastPublisher.publish(payload);

        } catch (Exception e) {
            log.error("auction-bid 처리 실패: {}", message, e);
            throw new RuntimeException(e);
        }
    }
}
