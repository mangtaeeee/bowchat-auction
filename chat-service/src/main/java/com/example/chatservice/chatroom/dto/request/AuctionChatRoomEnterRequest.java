package com.example.chatservice.chatroom.dto.request;

import com.example.chatservice.chatroom.entity.ChatRoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "경매 채팅방 입장 요청")
public class AuctionChatRoomEnterRequest extends ChatRoomEnterRequest {

    @Schema(description = "경매가 진행 중인 상품 ID", example = "10")
    private Long productId;

    @Override
    public ChatRoomType getRoomType() {
        return ChatRoomType.AUCTION;
    }
}
