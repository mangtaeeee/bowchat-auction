package com.example.chatservice.chatmessage.dto.response;

import com.example.chatservice.chatmessage.entity.ChatMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "채팅 메시지 응답")
public record ChatResponse(
        @Schema(description = "메시지 ID", example = "6610f8f86a4d632b52f68cb1")
        String id,
        @Schema(description = "채팅방 ID", example = "100")
        Long roomId,
        @Schema(description = "보낸 사용자 ID", example = "42")
        Long senderId,
        @Schema(description = "보낸 사용자 닉네임", example = "tester")
        String senderName,
        @Schema(description = "메시지 본문", example = "안녕하세요")
        String content,
        @Schema(description = "메시지 타입", example = "CHAT")
        String messageType,
        @Schema(description = "메시지 생성 시각", example = "2026-04-24T14:10:00")
        LocalDateTime createDate
) {
    public static ChatResponse from(ChatMessage chatMessage) {
        return ChatResponse.builder()
                .id(chatMessage.getId())
                .roomId(chatMessage.getRoomId())
                .senderId(chatMessage.getSenderId())
                .senderName(chatMessage.getSenderName())
                .content(chatMessage.getContent())
                .messageType(chatMessage.getMessageType())
                .createDate(chatMessage.getCreateDate())
                .build();
    }
}
