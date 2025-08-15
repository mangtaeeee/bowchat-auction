package com.example.bowchat.chatroom.dto;

import com.example.bowchat.chatroom.entity.ChatRoomParticipant;
import com.example.bowchat.chatroom.entity.ChatRoomParticipantRole;
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
                .participantId(chatRoomParticipant.getUser().getId())
                .role(chatRoomParticipant.getRole())
                .build();
    }
}
