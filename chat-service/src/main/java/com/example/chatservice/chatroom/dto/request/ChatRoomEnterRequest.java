package com.example.chatservice.chatroom.dto.request;

import com.example.chatservice.chatroom.entity.ChatRoomType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "roomType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AuctionChatRoomEnterRequest.class, name = "AUCTION"),
        @JsonSubTypes.Type(value = ProductChatRoomEnterRequest.class, name = "DIRECT"),
        @JsonSubTypes.Type(value = GroupChatRoomEnterRequest.class, name = "GROUP")
})
@Schema(
        description = "채팅방 입장 요청",
        discriminatorProperty = "roomType",
        oneOf = {
                AuctionChatRoomEnterRequest.class,
                ProductChatRoomEnterRequest.class,
                GroupChatRoomEnterRequest.class
        },
        discriminatorMapping = {
                @DiscriminatorMapping(value = "AUCTION", schema = AuctionChatRoomEnterRequest.class),
                @DiscriminatorMapping(value = "DIRECT", schema = ProductChatRoomEnterRequest.class),
                @DiscriminatorMapping(value = "GROUP", schema = GroupChatRoomEnterRequest.class)
        }
)
public abstract class ChatRoomEnterRequest {
    @Schema(description = "채팅방 타입", example = "AUCTION")
    public abstract ChatRoomType getRoomType();
}
