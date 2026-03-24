package com.example.chatservice.dto;


import com.example.chatservice.chatmessage.entity.MessageType;

public record ChatMessageRequest(
        Long roomId,
        String senderId,
        String senderName,
        MessageType type, // CHAT, JOIN, LEAVE
        String message
) {
}
