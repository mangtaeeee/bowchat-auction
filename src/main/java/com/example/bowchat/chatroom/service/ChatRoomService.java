package com.example.bowchat.chatroom.service;

import com.example.bowchat.chatroom.dto.ChatRoomCreateDTO;
import com.example.bowchat.chatroom.dto.ChatRoomResponse;
import com.example.bowchat.chatroom.entity.ChatRoom;
import com.example.bowchat.chatroom.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;

    @Transactional
    public void createChatRoom(ChatRoomCreateDTO request) {
        ChatRoom chatRoom = ChatRoom.builder()
                .name(request.chatRoomName())
                .participants(request.participants())
                .build();
        chatRoomRepository.save(chatRoom);
    }

    public ChatRoomResponse getChatRoom(Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findWithParticipantsById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방"));
        return ChatRoomResponse.from(chatRoom);
    }

}
