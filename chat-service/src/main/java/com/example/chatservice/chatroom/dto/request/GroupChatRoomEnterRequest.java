package com.example.chatservice.chatroom.dto.request;

import com.example.chatservice.chatroom.entity.ChatRoomType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GroupChatRoomEnterRequest extends ChatRoomEnterRequest {

    private String roomName;
    private Long userId;

    @Override
    public ChatRoomType getRoomType() {
        return ChatRoomType.GROUP;
    }
}
