package com.example.chatservice.chatroom.repository;

import com.example.chatservice.chatroom.entity.ChatRoom;
import com.example.chatservice.chatroom.entity.ChatRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT c FROM ChatRoom c LEFT JOIN FETCH c.participants WHERE c.id = :id")
    Optional<ChatRoom> findWithParticipantsById(@Param("id") Long id);


    // 경매 채팅방 - productId 기준 단일 방
    @Query("SELECT c FROM ChatRoom c LEFT JOIN FETCH c.participants WHERE c.type = :type AND c.product = :productId")
    Optional<ChatRoom> findByTypeAndProductWithParticipants(
            @Param("type") ChatRoomType type,
            @Param("productId") Long productId);

    // 상품 1:1 채팅방 - productId + buyerId 기준
    @Query("SELECT c FROM ChatRoom c LEFT JOIN FETCH c.participants WHERE c.type = :type AND c.product = :productId AND EXISTS (SELECT p FROM c.participants p WHERE p.userId = :userId)")
    Optional<ChatRoom> findByTypeAndProductAndUserIdWithParticipants(
            @Param("type") ChatRoomType type,
            @Param("productId") Long productId,
            @Param("userId") Long userId);
}

