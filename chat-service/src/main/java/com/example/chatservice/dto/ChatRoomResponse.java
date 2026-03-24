package com.example.chatservice.dto;

import com.example.bowchat.product.entity.SaleType;
import com.example.chatservice.chatroom.entity.ChatRoom;
import com.example.chatservice.chatroom.entity.ChatRoomType;
import lombok.Builder;

import java.util.List;

@Builder
public record ChatRoomResponse(
        Long roomId,
        String roomName,
        ChatRoomType type,
        SaleType saleType,  // AUCTION or DIRECT TODO ? 상품 측에 있는 Enum 타입으로 보임 ->
        List<ChatParticipantResponse> participants
) {
    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return ChatRoomResponse.builder()
                .roomId(chatRoom.getId())
                .roomName(chatRoom.getName())
                .type(chatRoom.getType())
                .saleType(chatRoom.getProduct().getSaleType())
                .participants(chatRoom.getParticipants().stream().map(ChatParticipantResponse::of).toList())
                .build();
    }
}