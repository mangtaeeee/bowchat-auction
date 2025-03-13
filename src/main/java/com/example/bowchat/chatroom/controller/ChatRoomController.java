package com.example.bowchat.chatroom.controller;

import com.example.bowchat.chatroom.dto.ChatRoomCreateDTO;
import com.example.bowchat.chatroom.dto.ChatRoomResponse;
import com.example.bowchat.chatroom.entity.ChatRoom;
import com.example.bowchat.chatroom.repository.ChatRoomRepository;
import com.example.bowchat.chatroom.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat/rooms")
public class ChatRoomController {


    private final ChatRoomService chatRoomService;

    @PostMapping
    public ResponseEntity<Void> createChatRoom(@RequestBody ChatRoomCreateDTO chatRoomCreateDTO) {
        chatRoomService.createChatRoom(chatRoomCreateDTO);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ChatRoomResponse> getChatRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(chatRoomService.getChatRoom(roomId));
    }


}
