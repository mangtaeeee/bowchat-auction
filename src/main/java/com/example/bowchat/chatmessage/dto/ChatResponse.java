package com.example.bowchat.chatmessage.dto;

import com.example.bowchat.chatmessage.entity.ChatMessage;
import com.example.bowchat.chatmessage.entity.MessageType;
import lombok.Builder;

@Builder
public record ChatResponse(
        String senderName,
        String content,
        MessageType type, // CHAT, JOIN, LEAVE
        long timestamp
) {
    public static ChatResponse from(ChatMessage entity) {
        return ChatResponse.builder()
                .senderName(entity.getSenderName())
                .content(entity.getContent())
                .type(entity.getMessageType())
                .timestamp(entity.getTimestamp())
                .build();
    }
}
