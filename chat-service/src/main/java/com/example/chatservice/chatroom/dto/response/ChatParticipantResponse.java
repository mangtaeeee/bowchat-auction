package com.example.chatservice.chatroom.dto.response;

import com.example.chatservice.chatroom.entity.ChatRoomParticipant;
import com.example.chatservice.chatroom.entity.ChatRoomParticipantRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "채팅방 참여자 정보")
public record ChatParticipantResponse(
        @Schema(description = "채팅방 ID", example = "100")
        Long chatRoomId,
        @Schema(description = "채팅방 이름", example = "auction room")
        String chatRoomName,
        @Schema(description = "참여자 사용자 ID", example = "42")
        Long participantId,
        @Schema(description = "참여자 역할", example = "MEMBER")
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
