package com.example.chatservice.websocket;

import com.example.bowchat.kafkastarter.event.EventMessage;
import com.example.bowchat.kafkastarter.event.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisChatBroadcastSubscriberTest {

    @Mock
    private ChatWebSocketHandler chatWebSocketHandler;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void onMessageBroadcastsToLocalSessions() throws Exception {
        EventMessage event = EventMessage.builder()
                .roomId(7L)
                .senderId(1L)
                .senderName("tester")
                .topicName(MessageType.CHAT.getTopicName())
                .messageType(MessageType.CHAT.name())
                .content("hello")
                .timestamp(1L)
                .build();
        String payload = "{\"roomId\":7}";
        Message message = new DefaultMessage(
                payload.getBytes(StandardCharsets.UTF_8),
                "chat:broadcast".getBytes(StandardCharsets.UTF_8)
        );
        when(objectMapper.readValue(anyString(), org.mockito.ArgumentMatchers.eq(EventMessage.class)))
                .thenReturn(event);

        RedisChatBroadcastSubscriber subscriber =
                new RedisChatBroadcastSubscriber(chatWebSocketHandler, objectMapper);

        subscriber.onMessage(message, null);

        verify(chatWebSocketHandler).broadcast(eq(7L), anyString());
    }
}
