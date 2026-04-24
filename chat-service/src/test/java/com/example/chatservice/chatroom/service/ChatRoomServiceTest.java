package com.example.chatservice.chatroom.service;

import com.example.chatservice.chatroom.dto.request.ChatRoomEnterRequest;
import com.example.chatservice.chatroom.dto.response.ChatRoomResponse;
import com.example.chatservice.chatroom.dto.response.EnterChatResponse;
import com.example.chatservice.chatroom.entity.ChatRoom;
import com.example.chatservice.chatroom.entity.ChatRoomParticipant;
import com.example.chatservice.chatroom.entity.ChatRoomType;
import com.example.chatservice.chatroom.repository.ChatRoomRepository;
import com.example.chatservice.exception.ChatErrorCode;
import com.example.chatservice.exception.ChatException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Test
    void enterChatRoomDelegatesToMatchingManagerWithAuthenticatedUserId() {
        TestChatRoomManager manager = new TestChatRoomManager();
        ChatRoomService chatRoomService = new ChatRoomService(List.of(manager), chatRoomRepository);

        EnterChatResponse response = chatRoomService.enterChatRoom(new TestEnterRequest(), 42L);

        assertThat(manager.lastUserId).isEqualTo(42L);
        assertThat(response.roomId()).isEqualTo(1L);
        assertThat(response.roomType()).isEqualTo(ChatRoomType.GROUP);
    }

    @Test
    void enterChatRoomRejectsUnsupportedRoomType() {
        ChatRoomService chatRoomService = new ChatRoomService(List.of(), chatRoomRepository);

        assertThatThrownBy(() -> chatRoomService.enterChatRoom(new TestEnterRequest(), 42L))
                .isInstanceOf(ChatException.class)
                .extracting(ex -> ((ChatException) ex).getErrorCode())
                .isEqualTo(ChatErrorCode.UNSUPPORTED_CHAT_ROOM_TYPE);
    }

    @Test
    void leaveChatRoomDeactivatesExistingParticipant() {
        ChatRoom chatRoom = ChatRoom.builder()
                .name("group")
                .type(ChatRoomType.GROUP)
                .build();
        chatRoom.registerOwner(1L);
        chatRoom.addOrActivateMember(42L);
        when(chatRoomRepository.findWithParticipantsById(100L)).thenReturn(Optional.of(chatRoom));

        ChatRoomService chatRoomService = new ChatRoomService(List.of(new TestChatRoomManager()), chatRoomRepository);
        chatRoomService.leaveChatRoom(100L, 42L);

        ChatRoomParticipant participant = chatRoom.getParticipants().stream()
                .filter(p -> p.getUserId().equals(42L))
                .findFirst()
                .orElseThrow();
        assertThat(participant.isActive()).isFalse();
    }

    @Test
    void getMyChatRoomsReturnsMappedRoomsWithOnlyActiveParticipants() {
        ChatRoom firstRoom = ChatRoom.builder()
                .name("auction room")
                .type(ChatRoomType.AUCTION)
                .build();
        firstRoom.registerOwner(1L);
        firstRoom.addOrActivateMember(42L);
        firstRoom.addOrActivateMember(43L);
        firstRoom.deactivateMember(43L);

        ChatRoom secondRoom = ChatRoom.builder()
                .name("direct room")
                .type(ChatRoomType.DIRECT)
                .build();
        secondRoom.registerOwner(2L);
        secondRoom.addOrActivateMember(42L);

        when(chatRoomRepository.findAllActiveRoomsByUserIdWithParticipants(42L))
                .thenReturn(List.of(firstRoom, secondRoom));

        ChatRoomService chatRoomService = new ChatRoomService(List.of(new TestChatRoomManager()), chatRoomRepository);

        List<ChatRoomResponse> responses = chatRoomService.getMyChatRooms(42L);

        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(ChatRoomResponse::roomName)
                .containsExactly("auction room", "direct room");
        assertThat(responses.get(0).participants())
                .extracting(participant -> participant.participantId())
                .containsExactlyInAnyOrder(1L, 42L);
    }

    private static final class TestEnterRequest extends ChatRoomEnterRequest {

        @Override
        public ChatRoomType getRoomType() {
            return ChatRoomType.GROUP;
        }
    }

    private static final class TestChatRoomManager implements ChatRoomManager<TestEnterRequest> {

        private Long lastUserId;

        @Override
        public ChatRoomType supportType() {
            return ChatRoomType.GROUP;
        }

        @Override
        public Class<TestEnterRequest> requestType() {
            return TestEnterRequest.class;
        }

        @Override
        public EnterChatResponse enterChatRoom(TestEnterRequest request, Long userId) {
            this.lastUserId = userId;
            return new EnterChatResponse(1L, ChatRoomType.GROUP, "group");
        }
    }
}
