package com.example.bowchat.chatmessage.entity;

import com.example.bowchat.kafka.ChatEvent;
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

    private String senderId;
    private String senderName;
    private String content;

    private MessageType messageType;

    private LocalDateTime createDate;
    private Long timestamp;

    public static ChatMessage from(ChatEvent event) {
        return ChatMessage.builder()
                .roomId(event.roomId())
                .senderId(event.senderId())
                .senderName(event.senderName())
                .content(event.content())
                .messageType(event.type())
                .createDate(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(event.timestamp()),
                        ZoneOffset.ofHours(9)
                ))
                .timestamp(event.timestamp())
                .build();
    }
}