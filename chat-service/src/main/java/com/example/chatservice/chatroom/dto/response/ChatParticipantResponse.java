package com.example.chatservice.chatroom.dto.response;

import com.example.chatservice.chatroom.entity.ChatRoomParticipant;
import com.example.chatservice.chatroom.entity.ChatRoomParticipantRole;
import lombok.Builder;

@Builder
public record ChatParticipantResponse(
        Long chatRoomId,
        String chatRoomName,
        Long participantId,
        ChatRoomParticipantRole role
) {
    public static ChatParticipantResponse of(ChatRoomParticipant chatRoomParticipant) {
        return ChatParticipantResponse.builder()
                .chatRoomId(chatRoomParticipant.getChatRoom().getId())
                .chatRoomName(chatRoomParticipant.getChatRoom().getName())
                .participantId(chatRoomParticipant.getUserId())
                .role(chatRoomParticipant.getRole())
                .build();
    }
}
