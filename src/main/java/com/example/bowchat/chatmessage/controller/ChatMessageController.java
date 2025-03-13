package com.example.bowchat.chatmessage.controller;

import com.example.bowchat.chatmessage.entity.ChatMessage;
import com.example.bowchat.chatmessage.repository.ChatMessageRepository;
import com.example.bowchat.chatmessage.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/chat/messages")
@RequiredArgsConstructor
public class ChatMessageController {
    private final ChatMessageService chatMessageService;

    @GetMapping("/{roomId}")
    public ResponseEntity<List<ChatMessage>> getMessages(@PathVariable Long roomId) {
        List<ChatMessage> messages = chatMessageService.findByChatMessages(roomId);
        return ResponseEntity.ok(messages);
    }
}
