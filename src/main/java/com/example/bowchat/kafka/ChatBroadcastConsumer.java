package com.example.bowchat.kafka;

import com.example.bowchat.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatBroadcastConsumer {

    private final WebSocketSessionManager sessionManager;

    @KafkaListener(topics = "chat-message", groupId = "chat-broadcast-group")
    public void broadcast(ChatEvent chatEvent) {
        sessionManager.broadcast(chatEvent.roomId(), chatEvent);
    }
}