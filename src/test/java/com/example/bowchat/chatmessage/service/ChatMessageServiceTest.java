package com.example.bowchat.chatmessage.service;

import com.example.bowchat.chatmessage.dto.ChatResponse;
import com.example.bowchat.chatmessage.entity.ChatMessage;
import com.example.bowchat.chatmessage.entity.MessageType;
import com.example.bowchat.chatmessage.repository.ChatMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ChatMessageServiceTest {

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @BeforeEach
    void setUp() {
        // 테스트용 데이터 초기화
        chatMessageRepository.deleteAll();

        ChatMessage message1 = ChatMessage.builder()
                .roomId(1L)
                .senderId("user1@example.com")
                .senderName("태윤")
                .messageType(MessageType.CHAT)
                .content("안녕하세요")
                .timestamp(Instant.now().toEpochMilli())
                .build();

        ChatMessage message2 = ChatMessage.builder()
                .roomId(1L)
                .senderId("user2@example.com")
                .senderName("홍길동")
                .messageType(MessageType.CHAT)
                .content("반갑습니다")
                .timestamp(Instant.now().toEpochMilli())
                .build();

        chatMessageRepository.save(message1);
        chatMessageRepository.save(message2);
    }

    @Test
    void findByChatMessages_shouldReturnChatResponses() {
        // when
        List<ChatResponse> responses = chatMessageService.findByChatMessages(1L);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).content()).isEqualTo("안녕하세요");
        assertThat(responses.get(1).content()).isEqualTo("반갑습니다");
    }
}