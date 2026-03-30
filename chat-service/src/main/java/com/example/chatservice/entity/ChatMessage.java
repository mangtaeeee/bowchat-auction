package com.example.chatservice.entity;

import com.example.bowchat.kafkastarter.event.EventMessage;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Document(collection = "chat_messages")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class ChatMessage {

    @Id
    private String id;
    private Long roomId;
    private Long senderId;
    private String senderName;
    private String content;
    private String messageType;
    private LocalDateTime createDate;
    private Long timestamp;

    public static ChatMessage from(EventMessage event) {
        return ChatMessage.builder()
                .roomId(event.roomId())
                .senderId(event.senderId())
                .senderName(event.senderName())
                .content(event.content())
                .messageType(event.messageType())
                .createDate(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(event.timestamp()),
                        ZoneOffset.ofHours(9)
                ))
                .timestamp(event.timestamp())
                .build();
    }
}