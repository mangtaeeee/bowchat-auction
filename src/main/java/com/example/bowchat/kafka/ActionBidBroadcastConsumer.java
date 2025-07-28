package com.example.bowchat.kafka;

import com.example.bowchat.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActionBidBroadcastConsumer {

    private final WebSocketSessionManager webSocketSessionManager;

    // 경매 입찰 메시지를 WebSocket으로 브로드캐스트
    @KafkaListener(topics = "action-bid", groupId = "action-bid-broadcast-group")
    public void broadcastBid(ChatEvent chatEvent) {
        webSocketSessionManager.broadcast(chatEvent);
    }
}
