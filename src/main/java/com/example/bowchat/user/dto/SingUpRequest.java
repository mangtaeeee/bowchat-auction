package com.example.bowchat.user.dto;

public record SingUpRequest(
        String email,
        String password,
        String nickName
) {
}
