package com.example.chatservice.chatmessage.controller;

import com.example.chatservice.auth.UserPrincipal;
import com.example.chatservice.chatmessage.dto.response.ChatResponse;
import com.example.chatservice.chatmessage.service.ChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Chat Message", description = "채팅 메시지 조회 API")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @Operation(summary = "채팅 메시지 조회", description = "채팅방의 메시지를 오래된 순서대로 조회합니다.")
    @GetMapping("/{roomId}")
    public ResponseEntity<List<ChatResponse>> getMessages(
            @Parameter(description = "조회할 채팅방 ID", example = "100")
            @PathVariable Long roomId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(chatMessageService.findMessages(roomId));
    }
}
