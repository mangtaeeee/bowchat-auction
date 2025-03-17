package com.example.bowchat.chatmessage.service;

import com.example.bowchat.chatmessage.dto.ChatMessageResponse;
import com.example.bowchat.chatmessage.entity.ChatMessage;
import com.example.bowchat.chatmessage.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    public List<ChatMessageResponse> findByChatMessages(Long chatRoomId){
        return chatMessageRepository.findByRoomId(chatRoomId)
                .stream().map(ChatMessageResponse::from).toList();
    }
}
