package com.example.bowchat.chatmessage.entity;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collation = "chat_messages")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class ChatMessage {

    @Id
    private String id;

    private Long roomId;

    private String sender;

    private String content;

    private LocalDateTime createDate;
}
