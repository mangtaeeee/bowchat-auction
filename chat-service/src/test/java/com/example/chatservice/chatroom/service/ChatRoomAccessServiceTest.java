package com.example.chatservice.chatroom.service;

import com.example.chatservice.chatroom.repository.ChatRoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomAccessServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Test
    void isActiveParticipantReturnsRepositoryResult() {
        when(chatRoomRepository.existsActiveParticipant(10L, 42L)).thenReturn(true);

        ChatRoomAccessService accessService = new ChatRoomAccessService(chatRoomRepository);

        assertThat(accessService.isActiveParticipant(10L, 42L)).isTrue();
    }
}
