package com.example.bowchat.chatmessage.service;

import com.example.bowchat.chatmessage.dto.ChatMessageDTO;
import com.example.bowchat.chatmessage.entity.ChatMessage;
import com.example.bowchat.chatmessage.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatConsumer {
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;


    @KafkaListener(topics = "chat-topic", groupId = "chat-group")
    @Transactional
    public void listen(ChatMessageDTO messageDTO) {
        saveChatMessage(messageDTO);
        // 소켓 메시지 전송
        messagingTemplate.convertAndSend("/topic/chat/" + messageDTO.roomId(), messageDTO);
    }

    private void saveChatMessage(ChatMessageDTO messageDTO) {
        ChatMessage chatMessage = ChatMessage.builder()
                .roomId(messageDTO.roomId())
                .sender(messageDTO.sender())
                .content(messageDTO.message())
                .createDate(LocalDateTime.now())
                .build();
        chatMessageRepository.save(chatMessage);
    }
}
