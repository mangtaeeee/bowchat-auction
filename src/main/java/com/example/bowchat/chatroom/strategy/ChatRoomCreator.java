package com.example.bowchat.chatroom.strategy;

import com.example.bowchat.chatroom.dto.ChatRoomResponse;
import com.example.bowchat.chatroom.entity.ChatRoomType;
import com.example.bowchat.user.entity.User;

public interface ChatRoomCreator<T> {
    ChatRoomType roomType();
    ChatRoomResponse createOrGet(T identifier, User user);
}