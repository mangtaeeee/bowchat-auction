package com.example.bowchat.kafka;

import com.example.bowchat.chatroom.service.ChatRoomService;
import com.example.bowchat.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventConsumer {

    private final ChatRoomService chatRoomService;
    private final WebSocketSessionManager sessionManager;

    // Kafka 에서 채팅 이벤트를 수신하고 처리
    @KafkaListener(topics = "chat-event", groupId = "chat-event-group")
    public void handleEvent(ChatEvent event) {
        switch (event.type()) {
            case ENTER -> {
                chatRoomService.activateParticipant(event.roomId(), event.senderId());
                sessionManager.broadcast(event);
                log.info("참여자 활성화 및 입장 알림 전송: {}", event);
            }
            case LEAVE -> {
                chatRoomService.deactivateParticipant(event.roomId(), event.senderId());
                sessionManager.broadcast(event);
                log.info("참여자 비활성화 및 퇴장 알림 전송: {}", event);
            }
            case SYSTEM -> {
                sessionManager.broadcast(event);
                log.info("시스템 메시지 전송: {}", event);
            }
            default -> log.warn("지원하지 않는 이벤트 타입: {}", event.type());
        }
    }
}
