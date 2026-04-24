package com.example.chatservice.chatroom.dto.request;

import com.example.chatservice.chatroom.entity.ChatRoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "상품 1:1 채팅방 입장 요청")
public class ProductChatRoomEnterRequest extends ChatRoomEnterRequest {

    @Schema(description = "채팅을 시작할 상품 ID", example = "10")
    private Long productId;

    @Override
    public ChatRoomType getRoomType() {
        return ChatRoomType.DIRECT;
    }
}
