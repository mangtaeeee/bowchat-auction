package com.example.chatservice.chatroom.dto.request;

import com.example.chatservice.chatroom.entity.ChatRoomType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AuctionChatRoomEnterRequest extends ChatRoomEnterRequest {

    private Long productId;
    @Override
    public ChatRoomType getRoomType() {
        return ChatRoomType.AUCTION;
    }
}
