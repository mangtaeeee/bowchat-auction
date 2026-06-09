package com.example.chatservice.chatroom.controller;

import com.example.chatservice.auth.UserPrincipal;
import com.example.chatservice.chatroom.dto.response.ChatRoomResponse;
import com.example.chatservice.chatroom.entity.ChatRoomType;
import com.example.chatservice.chatroom.service.ChatRoomService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomControllerTest {

    @Mock
    private ChatRoomService chatRoomService;

    @InjectMocks
    private ChatRoomController chatRoomController;

    @Test
    void getMyChatRoomsDelegatesUsingAuthenticatedUserId() {
        UserPrincipal user = new UserPrincipal(42L, "user@test.com", "tester", "USER");
        ChatRoomResponse response = ChatRoomResponse.builder()
                .roomId(100L)
                .roomName("auction room")
                .type(ChatRoomType.AUCTION)
                .participants(List.of())
                .build();
        when(chatRoomService.getMyChatRooms(42L)).thenReturn(List.of(response));

        List<ChatRoomResponse> body = chatRoomController.getMyChatRooms(user).getBody();

        assertThat(body).containsExactly(response);
        verify(chatRoomService).getMyChatRooms(42L);
    }
}
