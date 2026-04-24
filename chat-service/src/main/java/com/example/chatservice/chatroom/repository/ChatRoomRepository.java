package com.example.chatservice.chatroom.repository;

import com.example.chatservice.chatroom.entity.ChatRoom;
import com.example.chatservice.chatroom.entity.ChatRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
            FROM ChatRoomParticipant p
            WHERE p.chatRoom.id = :roomId
              AND p.userId = :userId
              AND p.isActive = true
            """)
    boolean existsActiveParticipant(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Query("SELECT c FROM ChatRoom c LEFT JOIN FETCH c.participants WHERE c.id = :id")
    Optional<ChatRoom> findWithParticipantsById(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT c
            FROM ChatRoomParticipant p
            JOIN p.chatRoom c
            LEFT JOIN FETCH c.participants
            WHERE p.userId = :userId
              AND p.isActive = true
            ORDER BY c.id DESC
            """)
    List<ChatRoom> findAllActiveRoomsByUserIdWithParticipants(@Param("userId") Long userId);

    @Query("SELECT c FROM ChatRoom c LEFT JOIN FETCH c.participants WHERE c.type = :type AND c.product = :productId")
    Optional<ChatRoom> findByTypeAndProductWithParticipants(
            @Param("type") ChatRoomType type,
            @Param("productId") Long productId);

    @Query("SELECT c FROM ChatRoom c LEFT JOIN FETCH c.participants WHERE c.type = :type AND c.product = :productId AND EXISTS (SELECT p FROM c.participants p WHERE p.userId = :userId)")
    Optional<ChatRoom> findByTypeAndProductAndUserIdWithParticipants(
            @Param("type") ChatRoomType type,
            @Param("productId") Long productId,
            @Param("userId") Long userId);
}
