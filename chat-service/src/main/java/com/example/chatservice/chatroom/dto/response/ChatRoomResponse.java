package com.example.chatservice.chatroom.dto.response;

import com.example.chatservice.chatroom.entity.ChatRoom;
import com.example.chatservice.chatroom.entity.ChatRoomType;
import lombok.Builder;

import java.util.List;

@Builder
public record ChatRoomResponse(
        Long roomId,
        String roomName,
        ChatRoomType type,
        List<ChatParticipantResponse> participants
) {
    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return ChatRoomResponse.builder()
                .roomId(chatRoom.getId())
                .roomName(chatRoom.getName())
                .type(chatRoom.getType())
                .participants(chatRoom.getParticipants().stream().map(ChatParticipantResponse::of).toList())
                .build();
    }
}