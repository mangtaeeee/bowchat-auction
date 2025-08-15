package com.example.bowchat.chatroom.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ChatRoomParticipantRole {

    OWNER("OWNER"),
    SELLER("SELLER"),
    MEMBER("MEMBER");

    private final String role;


}
