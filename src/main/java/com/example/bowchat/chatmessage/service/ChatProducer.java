package com.example.bowchat.chatmessage.service;

import com.example.bowchat.chatmessage.dto.ChatMessageDTO;
import com.example.bowchat.chatroom.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChatProducer {

    private final KafkaTemplate<String, ChatMessageDTO> kafkaTemplate;
    private final ChatRoomRepository chatRoomRepository;

    public void sendMessage(ChatMessageDTO messageDTO) {
        boolean exists = chatRoomRepository.existsById(messageDTO.roomId());
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 채팅방입니다.");
        }

        kafkaTemplate.send("chat-topic", messageDTO);
    }
}
