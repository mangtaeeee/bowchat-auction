package com.example.bowchat.chatmessage.dto;

public record ChatMessageDTO(
        Long roomId,
        String sender,
        String message

) {
}
