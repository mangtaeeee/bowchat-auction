package com.example.chatservice.websocket;

import com.example.bowchat.kafkastarter.event.EventMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisChatBroadcastSubscriber implements MessageListener {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            EventMessage event = objectMapper.readValue(payload, EventMessage.class);
            chatWebSocketHandler.broadcast(event.roomId(), payload);
        } catch (Exception e) {
            log.error("Redis 브로드캐스트 처리 실패", e);
        }
    }
}
