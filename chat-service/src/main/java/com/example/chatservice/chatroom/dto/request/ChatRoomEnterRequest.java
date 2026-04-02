package com.example.chatservice.chatroom.dto.request;

import com.example.chatservice.chatroom.entity.ChatRoomType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "roomType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AuctionChatRoomEnterRequest.class, name = "AUCTION"),
        @JsonSubTypes.Type(value = ProductChatRoomEnterRequest.class, name = "DIRECT"),
        @JsonSubTypes.Type(value = GroupChatRoomEnterRequest.class, name = "GROUP")
})
public abstract class ChatRoomEnterRequest {
    public abstract ChatRoomType getRoomType();
}
