package com.example.chatservice.chatroom.service;

import com.example.chatservice.chatroom.dto.request.ChatRoomEnterRequest;
import com.example.chatservice.chatroom.dto.response.ChatRoomResponse;
import com.example.chatservice.chatroom.dto.response.EnterChatResponse;
import com.example.chatservice.chatroom.entity.ChatRoomType;
import com.example.chatservice.chatroom.repository.ChatRoomRepository;
import com.example.chatservice.exception.ChatErrorCode;
import com.example.chatservice.exception.ChatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatRoomService {

    private final Map<ChatRoomType, ChatRoomManager<? extends ChatRoomEnterRequest>> managers;
    private final ChatRoomRepository chatRoomRepository;

    public ChatRoomService(List<ChatRoomManager<? extends ChatRoomEnterRequest>> managers,
                           ChatRoomRepository chatRoomRepository) {
        this.managers = managers.stream()
                .collect(Collectors.toMap(ChatRoomManager::supportType, Function.identity()));
        this.chatRoomRepository = chatRoomRepository;
        log.info("등록된 ChatRoomManager: {}", this.managers.keySet());
    }

    public EnterChatResponse enterChatRoom(ChatRoomEnterRequest request, Long userId) {
        ChatRoomManager<? extends ChatRoomEnterRequest> manager = resolveManager(request.getRoomType());
        return manager.enter(request, userId);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyChatRooms(Long userId) {
        return chatRoomRepository.findAllActiveRoomsByUserIdWithParticipants(userId).stream()
                .map(ChatRoomResponse::from)
                .toList();
    }

    public void leaveChatRoom(Long roomId, Long userId) {
        chatRoomRepository.findWithParticipantsById(roomId)
                .ifPresent(room -> room.deactivateMember(userId));
    }

    private ChatRoomManager<? extends ChatRoomEnterRequest> resolveManager(ChatRoomType type) {
        ChatRoomManager<? extends ChatRoomEnterRequest> manager = managers.get(type);
        if (manager == null) {
            throw new ChatException(ChatErrorCode.UNSUPPORTED_CHAT_ROOM_TYPE);
        }
        return manager;
    }
}
