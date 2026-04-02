package com.example.chatservice.chatmessage.service;

import com.example.bowchat.kafkastarter.event.EventMessage;
import com.example.chatservice.chatmessage.dto.response.ChatResponse;
import com.example.chatservice.chatmessage.entity.ChatMessage;
import com.example.chatservice.chatmessage.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    public List<ChatResponse> findMessages(Long roomId) {
        return chatMessageRepository.findByRoomIdOrderByTimestampAsc(roomId)
                .stream()
                .map(ChatResponse::from)
                .toList();
    }

    public void save(EventMessage event) {
        ChatMessage message = ChatMessage.from(event);
        chatMessageRepository.save(message);
        log.debug("메시지 저장: roomId={}, senderId={}", event.roomId(), event.senderId());
    }
}

