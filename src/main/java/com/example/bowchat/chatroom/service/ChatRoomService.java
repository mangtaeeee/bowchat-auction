package com.example.bowchat.chatroom.service;

import com.example.bowchat.chatroom.dto.ChatRoomCreateDTO;
import com.example.bowchat.chatroom.dto.ChatRoomResponse;
import com.example.bowchat.chatroom.entity.ChatRoom;
import com.example.bowchat.chatroom.repository.ChatRoomRepository;
import com.example.bowchat.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;

    @Transactional
    public void createChatRoom(ChatRoomCreateDTO request, User owner) {

        ChatRoom chatRoom = ChatRoom.builder()
                .name(request.chatRoomName())
                .owner(owner) // ✅ Owner 설정
                .build();

        chatRoomRepository.save(chatRoom);
        chatRoom.addParticipant(owner);

        log.info("채팅방 생성 완료: {}, 개설자={}", chatRoom.getName(), owner.getEmail());
    }

    public List<ChatRoomResponse> getAllChatRooms() {
        return chatRoomRepository.findAll().stream().map(ChatRoomResponse::from).toList();
    }

    public ChatRoomResponse getChatRoom(Long chatRoomId, User user) {
        ChatRoom chatRoom = chatRoomRepository.findWithParticipantsById(chatRoomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 채팅방입니다."));
        chatRoom.addParticipant(user);
        return ChatRoomResponse.from(chatRoom);
    }

}
