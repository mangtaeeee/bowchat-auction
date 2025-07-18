package com.example.bowchat.chatroom.controller;

import com.example.bowchat.chatroom.dto.ChatRoomCreateDTO;
import com.example.bowchat.chatroom.dto.ChatRoomResponse;
import com.example.bowchat.chatroom.service.ChatRoomService;
import com.example.bowchat.user.entity.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat/rooms")
@Slf4j
public class ChatRoomController {


    private final ChatRoomService chatRoomService;

    @PostMapping
    public ResponseEntity<Void> createChatRoom(
            @RequestBody ChatRoomCreateDTO chatRoomCreateDTO,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        log.info("Create chat room: {}", chatRoomCreateDTO);
        chatRoomService.createChatRoom(chatRoomCreateDTO, principalDetails.getUser());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<ChatRoomResponse>> getAllChatRooms() {
        return ResponseEntity.ok(chatRoomService.getAllChatRooms());
    }

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


}
