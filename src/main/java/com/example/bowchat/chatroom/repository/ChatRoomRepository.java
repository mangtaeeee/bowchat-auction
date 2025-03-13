package com.example.bowchat.chatroom.repository;

import com.example.bowchat.chatroom.entity.ChatRoom;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @EntityGraph(attributePaths = "participants")
    Optional<ChatRoom> findWithParticipantsById(Long id);
}
