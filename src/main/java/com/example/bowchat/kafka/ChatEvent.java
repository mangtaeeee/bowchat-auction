package com.example.bowchat.kafka;

import com.example.bowchat.chatmessage.entity.MessageType;
import lombok.Builder;

import java.time.Instant;

// 카프카를 통해 전송되는 채팅 공통 메시지 구조 정의
@Builder
public record ChatEvent(
        Long roomId,
        String senderId,
        String senderName,
        MessageType type,
        String content,   // 실제 메시지 내용
        Long timestamp
) {
    public static ChatEvent enrichChatEvent(Long roomId, ChatEvent original) {
        return ChatEvent.builder()
                .roomId(roomId)
                .senderId(original.senderId())
                .senderName(original.senderName())
                .type(MessageType.CHAT)
                .content(original.content())
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }
}
