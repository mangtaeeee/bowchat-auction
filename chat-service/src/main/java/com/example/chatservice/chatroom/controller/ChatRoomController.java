package com.example.chatservice.chatroom.controller;

import com.example.chatservice.auth.UserPrincipal;
import com.example.chatservice.chatroom.dto.request.ChatRoomEnterRequest;
import com.example.chatservice.chatroom.dto.response.ChatRoomResponse;
import com.example.chatservice.chatroom.dto.response.EnterChatResponse;
import com.example.chatservice.chatroom.service.ChatRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat Room", description = "채팅방 조회, 입장, 나가기 API")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @Operation(summary = "내 채팅방 목록 조회", description = "현재 로그인 사용자가 active participant 인 채팅방 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<ChatRoomResponse>> getMyChatRooms(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal user
    ) {
        log.info("내 채팅방 목록 조회 요청: userId={}", user.userId());
        return ResponseEntity.ok(chatRoomService.getMyChatRooms(user.userId()));
    }

    @Operation(summary = "채팅방 입장", description = "roomType 에 따라 경매 채팅방, 상품 1:1 채팅방, 그룹 채팅방에 입장하거나 새로 생성합니다.")
    @PostMapping("/enter")
    public ResponseEntity<EnterChatResponse> enterChatRoom(
            @RequestBody ChatRoomEnterRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal user
    ) {
        log.info("채팅방 입장 요청: type={}, userId={}", request.getRoomType(), user.userId());
        EnterChatResponse response = chatRoomService.enterChatRoom(request, user.userId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "채팅방 나가기", description = "현재 로그인 사용자를 채팅방의 비활성 참여자로 변경합니다.")
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leave(
            @Parameter(description = "나갈 채팅방 ID", example = "100")
            @PathVariable Long roomId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserPrincipal user
    ) {
        log.info("채팅방 나가기 요청: roomId={}, userId={}", roomId, user.userId());
        chatRoomService.leaveChatRoom(roomId, user.userId());
        return ResponseEntity.ok().build();
    }
}
