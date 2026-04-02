package com.example.chatservice.chatroom.dto.request;

import com.example.chatservice.chatroom.entity.ChatRoomType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductChatRoomEnterRequest extends ChatRoomEnterRequest {

    private Long productId;
    private Long buyerId;

    @Override
    public ChatRoomType getRoomType() {
        return ChatRoomType.DIRECT;
    }
}
