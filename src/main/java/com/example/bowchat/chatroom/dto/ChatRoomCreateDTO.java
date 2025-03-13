package com.example.bowchat.chatroom.dto;

import java.util.List;

public record ChatRoomCreateDTO(
        String chatRoomName,
        List<String> participants
) {
}
