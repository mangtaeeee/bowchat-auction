package com.example.chatservice.chatmessage.controller;

import com.example.chatservice.auth.UserPrincipal;
import com.example.chatservice.chatmessage.dto.response.ChatResponse;
import com.example.chatservice.chatmessage.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat/messages")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @GetMapping("/{roomId}")
    public ResponseEntity<List<ChatResponse>> getMessages(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(chatMessageService.findMessages(roomId));
    }
}