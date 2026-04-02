package com.example.chatservice.chatmessage.repository;

import com.example.chatservice.chatmessage.entity.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    // roomId 기준 시간순 조회
    List<ChatMessage> findByRoomIdOrderByTimestampAsc(Long roomId);

    // 페이징 조회 (최근 N개)
    @Query(value = "{ 'roomId': ?0 }", sort = "{ 'timestamp': -1 }")
    List<ChatMessage> findRecentMessages(Long roomId, org.springframework.data.domain.Pageable pageable);
}
