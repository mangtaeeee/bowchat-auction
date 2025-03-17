package com.example.bowchat.chatmessage.dto;

import com.example.bowchat.chatmessage.entity.ChatMessage;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ChatMessageResponse(

        String id,
        Long roomId,
        String sender,
        String message,
        LocalDateTime createDate
) {
    public static ChatMessageResponse from(ChatMessage chatMessage) {
        return ChatMessageResponse.builder()
                .id(chatMessage.getId())
                .roomId(chatMessage.getRoomId())
                .message(chatMessage.getContent())
                .sender(chatMessage.getSender())
                .createDate(chatMessage.getCreateDate())
                .build();
    }
}
