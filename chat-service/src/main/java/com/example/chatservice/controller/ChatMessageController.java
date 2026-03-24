package com.example.chatservice.controller;

import com.example.chatservice.dto.ChatResponse;
import com.example.chatservice.service.ChatMessageService;
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
    public ResponseEntity<List<ChatResponse>> getMessages(@PathVariable Long roomId) {
        List<ChatResponse> messages = chatMessageService.findByChatMessages(roomId);
        return ResponseEntity.ok(messages);
    }
}
