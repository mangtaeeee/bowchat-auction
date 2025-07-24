package com.example.bowchat.chatroom.service;

import com.example.bowchat.chatroom.dto.ChatRoomCreateDTO;
import com.example.bowchat.chatroom.dto.ChatRoomResponse;
import com.example.bowchat.chatroom.entity.ChatRoom;
import com.example.bowchat.chatroom.entity.ChatRoomParticipant;
import com.example.bowchat.chatroom.repository.ChatRoomRepository;
import com.example.bowchat.user.entity.User;
import com.example.bowchat.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Transactional
    public void createChatRoom(ChatRoomCreateDTO request, User owner) {

        ChatRoom chatRoom = ChatRoom.builder()
                .name(request.chatRoomName())
                .owner(owner)
                .build();

        chatRoomRepository.save(chatRoom);
        chatRoom.registerOwner(owner);

        log.info("채팅방 생성 완료: {}, 개설자={}", chatRoom.getName(), owner.getEmail());
    }

    public List<ChatRoomResponse> getAllChatRooms() {
        return chatRoomRepository.findAll().stream().map(ChatRoomResponse::from).toList();
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
    @Transactional
    public void activateParticipant(Long roomId, Long senderId) {
        ChatRoom chatRoom = getChatRoomWithParticipants(roomId);
        User user = getUser(senderId);
        // 참여자 중 해당 유저를 찾아 활성화 처리
        chatRoom.addOrActivateMember(user);
    }

    @Transactional
    public void deactivateParticipant(Long roomId, Long senderId) {
        ChatRoom chatRoom = getChatRoomWithParticipants(roomId);

        User user = getUser(senderId);

        // 참여자 중 해당 유저를 찾아 비활성화 처리
        chatRoom.getParticipants().stream().filter(p -> p.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "참여자가 아닙니다."))
                .deactivate();
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
