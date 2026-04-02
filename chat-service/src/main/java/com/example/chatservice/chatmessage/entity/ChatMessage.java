package com.example.chatservice.chatmessage.entity;

import com.example.bowchat.kafkastarter.event.EventMessage;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Document(collection = "chat_messages")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    private String id;

    @Indexed  // roomId 기준 조회가 많으므로 인덱스 추가
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
