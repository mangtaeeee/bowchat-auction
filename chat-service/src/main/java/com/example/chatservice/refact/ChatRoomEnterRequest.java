package com.example.chatservice.refact;

import com.example.chatservice.entity.ChatRoomType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;

@Getter
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "roomType",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AuctionChatRoomEnterRequest.class,    name = "AUCTION"),
        @JsonSubTypes.Type(value = ProductChatRoomEnterRequest.class,   name = "DIRECT"),
        @JsonSubTypes.Type(value = GroupChatRoomEnterRequest.class,     name = "GROUP"),
})
public abstract class ChatRoomEnterRequest {

    /** 입장할 채팅방의 고유 ID */
    private Long roomId;

    /** 채팅방 타입 — Jackson 역직렬화 식별자로도 사용 */
    private ChatRoomType roomType;
}
