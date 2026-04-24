package com.example.chatservice.chatroom.dto.response;

import com.example.chatservice.chatroom.entity.ChatRoomType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅방 입장 응답")
public record EnterChatResponse(
        @Schema(description = "입장한 채팅방 ID", example = "100")
        Long roomId,
        @Schema(description = "채팅방 타입", example = "AUCTION")
        ChatRoomType roomType,
        @Schema(description = "채팅방 이름", example = "auction room")
        String roomName
) {
}
