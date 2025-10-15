package com.example.bowchat.chatroom.service;

import com.example.bowchat.chatroom.entity.ChatRoom;
import com.example.bowchat.chatroom.repository.ChatRoomRepository;
import com.example.bowchat.product.entity.Product;
import com.example.bowchat.user.entity.User;
import com.example.bowchat.user.repository.UserRepository;
import com.example.bowchat.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks
    private ChatRoomService chatRoomService;
    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // 테스트: getChatRoom 호출 시 chatRoomRepository.findWithParticipantsById()가 호출되고 chatRoom.addOrActivateMember(user)가 실행된다
    @Test
    void testGetChatRoom() {
        // Given
        Long chatRoomId = 1L;
        User user = new User();
        ChatRoom chatRoom = mock(ChatRoom.class);
        when(chatRoomRepository.findWithParticipantsById(chatRoomId))
                .thenReturn(Optional.of(chatRoom));
        when(chatRoom.getProduct()).thenReturn(mock(Product.class));
        // When
        chatRoomService.getChatRoom(chatRoomId, user);

        // Then
        verify(chatRoomRepository).findWithParticipantsById(chatRoomId);
        verify(chatRoom).addOrActivateMember(user);
    }
    // 테스트: 참여자가 아닌 사용자가 채팅방 나가기를 시도하면 ResponseStatusException 발생
    @Test
    void testLeaveChatRoom_NotParticipant() {
        // Given
        Long chatRoomId = 1L;
        User user = new User();
        ChatRoom chatRoom = mock(ChatRoom.class);
        when(chatRoomRepository.findWithParticipantsById(chatRoomId))
                .thenReturn(Optional.of(chatRoom));
        when(chatRoom.getParticipants()).thenReturn(List.of());

        // When & Then
        try {
            chatRoomService.leaveChatRoom(chatRoomId, user);
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
            assertEquals("참여자가 아닙니다.", e.getReason());
        }

        verify(chatRoomRepository).findWithParticipantsById(chatRoomId);
    }


}