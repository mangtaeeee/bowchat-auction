package com.example.bowchat.chatroom.service;

import com.example.bowchat.chatroom.dto.ChatRoomResponse;
import com.example.bowchat.chatroom.entity.ChatRoom;
import com.example.bowchat.chatroom.entity.ChatRoomParticipant;
import com.example.bowchat.chatroom.entity.ChatRoomType;
import com.example.bowchat.chatroom.repository.ChatRoomRepository;
import com.example.bowchat.chatroom.strategy.ChatRoomCreator;
import com.example.bowchat.user.entity.User;
import com.example.bowchat.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final List<ChatRoomCreator<?>> creators;

    private Map<ChatRoomType, ChatRoomCreator<?>> creatorMap;

    @PostConstruct
    private void init() {
        creatorMap = creators.stream()
                .collect(Collectors.toMap(ChatRoomCreator::roomType, Function.identity()));
    }

    @Transactional
    public ChatRoomResponse getChatRoom(Long chatRoomId, User user) {

        ChatRoom chatRoom = getChatRoomWithParticipants(chatRoomId);

        chatRoom.addOrActivateMember(user);
        return ChatRoomResponse.from(chatRoom);
    }

    @Transactional
    public void leaveChatRoom(Long chatRoomId, User user) {
        ChatRoom chatRoom = getChatRoomWithParticipants(chatRoomId);

        List<ChatRoomParticipant> matchingParticipants = chatRoom.getParticipants().stream()
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .toList();

        if (matchingParticipants.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "참여자가 아닙니다.");
        }

        matchingParticipants.forEach(ChatRoomParticipant::deactivate);
    }

    //카프카 이벤트를 통해 참여자 활성 처리
    @Transactional
    public void activateParticipant(Long roomId, Long senderId) {
        ChatRoom chatRoom = getChatRoomWithParticipants(roomId);
        User user = getUser(senderId);
        // 참여자 중 해당 유저를 찾아 활성화 처리
        chatRoom.addOrActivateMember(user);
    }

    // 카프카 이벤트를 통해 참여자 비활성 처리
    @Transactional
    public void deactivateParticipant(Long roomId, Long senderId) {
        ChatRoom chatRoom = getChatRoomWithParticipants(roomId);

        User user = getUser(senderId);

        // 참여자 중 해당 유저를 찾아 비활성화 처리
        chatRoom.deactivateMember(user);
    }

    @Transactional
    public <I> ChatRoomResponse createOrGetChatRoom(
            ChatRoomType type, I identifier, User user
    ) {
        @SuppressWarnings("unchecked")
        ChatRoomCreator<I> creator = (ChatRoomCreator<I>) creatorMap.get(type);
        if (creator == null) {
            throw new IllegalArgumentException("지원하지 않는 타입: " + type);
        }
        return creator.createOrGet(identifier, user);
    }

    private ChatRoom getChatRoomWithParticipants(Long roomId) {
        return chatRoomRepository.findWithParticipantsById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 채팅방입니다."));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));
    }


}
