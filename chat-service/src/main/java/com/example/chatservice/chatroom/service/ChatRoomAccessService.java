package com.example.chatservice.chatroom.service;

import com.example.chatservice.chatroom.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRoomAccessService {

    private final ChatRoomRepository chatRoomRepository;

    public boolean isActiveParticipant(Long roomId, Long userId) {
        return chatRoomRepository.existsActiveParticipant(roomId, userId);
    }
}
