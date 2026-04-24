package com.example.chatservice.chatroom.dto.response;

import com.example.chatservice.chatroom.entity.ChatRoom;
import com.example.chatservice.chatroom.entity.ChatRoomParticipant;
import com.example.chatservice.chatroom.entity.ChatRoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "채팅방 목록/상세 응답")
public record ChatRoomResponse(
        @Schema(description = "채팅방 ID", example = "100")
        Long roomId,
        @Schema(description = "채팅방 이름", example = "auction room")
        String roomName,
        @Schema(description = "채팅방 타입", example = "AUCTION")
        ChatRoomType type,
        @Schema(description = "활성 참여자 목록")
        List<ChatParticipantResponse> participants
) {
    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return ChatRoomResponse.builder()
                .roomId(chatRoom.getId())
                .roomName(chatRoom.getName())
                .type(chatRoom.getType())
                .participants(chatRoom.getParticipants().stream()
                        .filter(ChatRoomParticipant::isActive)
                        .map(ChatParticipantResponse::of)
                        .toList())
                .build();
    }
}
