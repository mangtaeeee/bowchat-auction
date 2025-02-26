package com.example.bowchat.chatmessage.repository;

import com.example.bowchat.chatmessage.entity.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findByRoomId(Long roomId);
}
