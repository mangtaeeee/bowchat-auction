package com.example.chatservice.chatroom.dto.response;

import com.example.chatservice.chatroom.entity.ChatRoomType;

public record EnterChatResponse(
        Long roomId,
        ChatRoomType roomType,
        String roomName
) {
}
