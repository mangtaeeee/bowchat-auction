package com.example.bowchat.chatroom.dto;

import com.example.bowchat.chatroom.entity.ChatRoom;
import lombok.Builder;

import java.util.List;

@Builder
public record ChatRoomResponse(
        Long roomId,
        String roomName,
        List<String> participants
) {
    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return ChatRoomResponse.builder()
                .roomId(chatRoom.getId())
                .roomName(chatRoom.getName())
                .participants(List.copyOf(chatRoom.getParticipants()))
                .build();
    }
}
