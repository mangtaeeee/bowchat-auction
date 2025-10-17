package com.example.bowchat.chatroom.controller;

import com.example.bowchat.chatroom.dto.ChatRoomResponse;
import com.example.bowchat.chatroom.entity.ChatRoomType;
import com.example.bowchat.chatroom.service.ChatRoomService;
import com.example.bowchat.user.entity.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
@Slf4j
public class ChatRoomController {


    private final ChatRoomService chatRoomService;

    @GetMapping("/{roomId}")
    public ResponseEntity<ChatRoomResponse> getChatRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return ResponseEntity.ok(chatRoomService.getChatRoom(roomId,principalDetails.getUser()));
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveChatRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        chatRoomService.leaveChatRoom(roomId, principalDetails.getUser());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{type}")
    public ResponseEntity<ChatRoomResponse> openChatRoomByType(
            @PathVariable ChatRoomType type,
            @RequestParam Long productId,
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        log.info("openChatRoomByType ");
        return ResponseEntity.ok(chatRoomService.createOrGetChatRoom(
                type,
                productId,
                principal.getUser()
        ));
    }


}
