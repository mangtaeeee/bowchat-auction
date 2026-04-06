package com.example.chatservice.chatroom.controller;

import com.example.chatservice.auth.UserPrincipal;
import com.example.chatservice.chatroom.dto.request.ChatRoomEnterRequest;
import com.example.chatservice.chatroom.dto.response.EnterChatResponse;
import com.example.chatservice.chatroom.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
@Slf4j
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @PostMapping("/enter")
    public ResponseEntity<EnterChatResponse> enterChatRoom(
            @RequestBody ChatRoomEnterRequest request,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        log.info("채팅방 입장 요청: type={}, userId={}", request.getRoomType(), user.userId());
        EnterChatResponse response = chatRoomService.enterChatRoom(request, user.userId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leave(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        log.info("채팅방 퇴장: roomId={}, userId={}", roomId, user.userId());
        chatRoomService.leaveChatRoom(roomId, user.userId());
        return ResponseEntity.ok().build();
    }
}
