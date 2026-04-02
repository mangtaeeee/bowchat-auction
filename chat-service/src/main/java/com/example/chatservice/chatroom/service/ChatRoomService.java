package com.example.chatservice.chatroom.service;

import com.example.chatservice.chatroom.dto.request.ChatRoomEnterRequest;
import com.example.chatservice.chatroom.dto.response.EnterChatResponse;
import com.example.chatservice.chatroom.entity.ChatRoomType;
import com.example.chatservice.chatroom.repository.ChatRoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatRoomService {

    private final Map<ChatRoomType, ChatRoomManager<? extends ChatRoomEnterRequest>> managers;
    private final ChatRoomRepository chatRoomRepository;

    // 생성자 주입으로 모든 Manager 자동 등록
    public ChatRoomService(List<ChatRoomManager<? extends ChatRoomEnterRequest>> managers,
                           ChatRoomRepository chatRoomRepository) {
        this.managers = managers.stream()
                .collect(Collectors.toMap(ChatRoomManager::supportType, Function.identity()));
        this.chatRoomRepository = chatRoomRepository;
        log.info("등록된 ChatRoomManager: {}", this.managers.keySet());
    }

    public EnterChatResponse enterChatRoom(ChatRoomEnterRequest request) {
        ChatRoomManager<? extends ChatRoomEnterRequest> manager = resolveManager(request.getRoomType());
        return manager.enter(request);
    }

    public void leaveChatRoom(Long roomId, Long userId) {
        chatRoomRepository.findWithParticipantsById(roomId)
                .ifPresent(room -> room.deactivateMember(userId));
    }

    private ChatRoomManager<? extends ChatRoomEnterRequest> resolveManager(ChatRoomType type) {
        ChatRoomManager<? extends ChatRoomEnterRequest> manager = managers.get(type);
        if (manager == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "지원하지 않는 채팅방 타입입니다: " + type);
        }
        return manager;
    }
}
