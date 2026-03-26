package com.example.chatservice.refact;

import com.example.chatservice.entity.ChatRoomType;

public interface ChatRoomManager {
    ChatRoomType supportType();
    ChatEnterResponse enterChatRoom(ChatRoomEnterRequest request);
}
