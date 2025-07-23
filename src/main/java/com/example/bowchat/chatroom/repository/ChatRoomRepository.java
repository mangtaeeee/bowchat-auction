package com.example.bowchat.chatroom.repository;

import com.example.bowchat.chatroom.entity.ChatRoom;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("SELECT c FROM ChatRoom c LEFT JOIN FETCH c.participants WHERE c.id = :id")
    Optional<ChatRoom> findWithParticipantsById(@Param("id") Long id);
}
