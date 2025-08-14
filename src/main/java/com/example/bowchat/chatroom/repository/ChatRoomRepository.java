package com.example.bowchat.chatroom.repository;

import com.example.bowchat.chatroom.entity.ChatRoom;
import com.example.bowchat.chatroom.entity.ChatRoomType;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("SELECT c FROM ChatRoom c LEFT JOIN FETCH c.participants WHERE c.id = :id")
    Optional<ChatRoom> findWithParticipantsById(@Param("id") Long id);

    // 채팅방 타입, product.id 와 participants.user.id 가 일치하는 방 조회
    Optional<ChatRoom> findByTypeAndProduct_IdAndParticipants_User_Id(
            ChatRoomType type,
            Long productId,
            Long buyerId
    );

    Optional<ChatRoom> findByTypeAndProduct_Id(
            ChatRoomType type,
            Long productId
    );

}
