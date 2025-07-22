package com.example.bowchat.kafka;

import com.example.bowchat.chatmessage.dto.ChatMessageRequest;
import com.example.bowchat.chatmessage.entity.MessageType;
import lombok.Builder;

// 카프카를 통해 전송되는 채팅 공통 메시지 구조 정의
@Builder
public record ChatEvent(
        Long roomId,
        String senderId,
        String senderName,
        MessageType type,
        String payload,   // 실제 메시지 내용
        Long timestamp
) {
    public static ChatEvent fromRequest(ChatMessageRequest request) {
        return ChatEvent.builder()
                .roomId(request.roomId())
                .senderId(request.senderId())
                .senderName(request.senderName())
                .type(request.type())
                .payload(request.message())
                .timestamp(System.currentTimeMillis()) // 현재 시간으로 타임스탬프 설정
                .build();
    }
}
