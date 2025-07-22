package com.example.bowchat.chatmessage.dto;

import com.example.bowchat.chatmessage.entity.MessageType;

public record ChatMessageRequest(
        Long roomId,
        String senderId,
        String senderName,
        MessageType type, // CHAT, JOIN, LEAVE
        String message
) {
}
