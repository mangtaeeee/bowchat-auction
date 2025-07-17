package com.example.bowchat.chatroom.dto;

import com.example.bowchat.chatroom.entity.ChatRoomParticipant;
import lombok.Builder;

@Builder
public record ChatParticipantResponse(
        Long chatRoomId,
        String chatRoomName,
        Long participantId
) {
    public static ChatParticipantResponse of(ChatRoomParticipant chatRoomParticipant) {
        return ChatParticipantResponse.builder()
                .chatRoomId(chatRoomParticipant.getChatRoom().getId())
                .chatRoomName(chatRoomParticipant.getChatRoom().getName())
                .participantId(chatRoomParticipant.getUser().getId())
                .build();
    }
}
