package com.example.chatservice.chatroom.service;

import com.example.chatservice.chatroom.dto.request.ChatRoomEnterRequest;
import com.example.chatservice.chatroom.dto.response.EnterChatResponse;
import com.example.chatservice.chatroom.entity.ChatRoom;
import com.example.chatservice.chatroom.entity.ChatRoomParticipant;
import com.example.chatservice.chatroom.entity.ChatRoomType;
import com.example.chatservice.chatroom.repository.ChatRoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    // 채팅방 서비스는 roomType별 매니저 선택과 leave 처리만 검증하면 된다.
    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Test
    void enterChatRoomDelegatesToMatchingManagerWithAuthenticatedUserId() {
        // given: GROUP 타입을 처리하는 테스트용 매니저를 등록한다.
        TestChatRoomManager manager = new TestChatRoomManager();
        ChatRoomService chatRoomService = new ChatRoomService(List.of(manager), chatRoomRepository);

        EnterChatResponse response = chatRoomService.enterChatRoom(new TestEnterRequest(), 42L);

        // then: 컨트롤러에서 넘긴 인증 사용자 id가 그대로 매니저까지 전달돼야 한다.
        assertThat(manager.lastUserId).isEqualTo(42L);
        assertThat(response.roomId()).isEqualTo(1L);
        assertThat(response.roomType()).isEqualTo(ChatRoomType.GROUP);
    }

    @Test
    void enterChatRoomRejectsUnsupportedRoomType() {
        // given: 어떤 매니저도 등록하지 않으면 해당 타입을 처리할 수 없다.
        ChatRoomService chatRoomService = new ChatRoomService(List.of(), chatRoomRepository);

        // when/then: 지원하지 않는 타입은 400으로 거절해야 한다.
        assertThatThrownBy(() -> chatRoomService.enterChatRoom(new TestEnterRequest(), 42L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void leaveChatRoomDeactivatesExistingParticipant() {
        // given: 이미 참여 중인 사용자가 포함된 채팅방을 만든다.
        ChatRoom chatRoom = ChatRoom.builder()
                .name("group")
                .type(ChatRoomType.GROUP)
                .build();
        chatRoom.registerOwner(1L);
        chatRoom.addOrActivateMember(42L);
        when(chatRoomRepository.findWithParticipantsById(100L)).thenReturn(Optional.of(chatRoom));

        ChatRoomService chatRoomService = new ChatRoomService(List.of(new TestChatRoomManager()), chatRoomRepository);
        chatRoomService.leaveChatRoom(100L, 42L);

        // then: leave 이후에는 해당 참여자의 active 상태가 false여야 한다.
        ChatRoomParticipant participant = chatRoom.getParticipants().stream()
                .filter(p -> p.getUserId().equals(42L))
                .findFirst()
                .orElseThrow();
        assertThat(participant.isActive()).isFalse();
    }

    // 타입 라우팅 테스트용 요청 객체다.
    private static final class TestEnterRequest extends ChatRoomEnterRequest {

        @Override
        public ChatRoomType getRoomType() {
            return ChatRoomType.GROUP;
        }
    }

    // 실제 매니저 대신, 전달된 userId를 기록하는 최소 구현체다.
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
