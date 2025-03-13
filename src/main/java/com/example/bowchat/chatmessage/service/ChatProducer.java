package com.example.bowchat.chatmessage.service;

import com.example.bowchat.chatmessage.dto.ChatMessageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatProducer {

    private final KafkaTemplate<String, ChatMessageDTO> kafkaTemplate;

    public void sendMessage(ChatMessageDTO messageDTO) {
        kafkaTemplate.send("chat-topic", messageDTO);
    }
}
