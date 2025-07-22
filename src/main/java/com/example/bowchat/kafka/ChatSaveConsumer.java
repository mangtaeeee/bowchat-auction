package com.example.bowchat.kafka;

import com.example.bowchat.chatmessage.entity.ChatMessage;
import com.example.bowchat.chatmessage.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


// Kafka 메시지를 수신하여 채팅 메시지를 저장하는 서비스
@Service
@RequiredArgsConstructor
public class ChatSaveConsumer {

    private final ChatMessageRepository chatMessageRepository;

    @KafkaListener(topics = "chat-topic", groupId = "chat-save-group")
    @Transactional
    public void save(ChatEvent chatEvent) {
        ChatMessage chatMessage = ChatMessage.from(chatEvent);
        chatMessageRepository.save(chatMessage);
    }
}
