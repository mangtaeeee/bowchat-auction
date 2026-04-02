package com.example.chatservice.chatmessage.dto.response;

import com.example.chatservice.chatmessage.entity.ChatMessage;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ChatResponse(
        String id,
        Long roomId,
        Long senderId,
        String senderName,
        String content,
        String messageType,
        LocalDateTime createDate
) {
    public static ChatResponse from(ChatMessage chatMessage) {
        return ChatResponse.builder()
                .id(chatMessage.getId())
                .roomId(chatMessage.getRoomId())
                .senderId(chatMessage.getSenderId())
                .senderName(chatMessage.getSenderName())
                .content(chatMessage.getContent())
                .messageType(chatMessage.getMessageType())
                .createDate(chatMessage.getCreateDate())
                .build();
    }
}
