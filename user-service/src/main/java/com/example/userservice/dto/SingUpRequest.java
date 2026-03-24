package com.example.userservice.dto;

public record SingUpRequest(
        String email,
        String password,
        String nickName
) {
}
