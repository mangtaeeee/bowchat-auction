package com.example.bowchat.chatroom.dto;

import com.example.bowchat.chatroom.entity.ChatRoom;
import com.example.bowchat.chatroom.entity.ChatRoomType;
import com.example.bowchat.product.entity.SaleType;
import lombok.Builder;

import java.util.List;

@Builder
public record ChatRoomResponse(
        Long roomId,
        String roomName,
        ChatRoomType type,
        SaleType saleType,  // AUCTION or DIRECT
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