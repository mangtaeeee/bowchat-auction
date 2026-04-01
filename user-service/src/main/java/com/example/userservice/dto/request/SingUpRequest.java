package com.example.userservice.dto.request;

public record SingUpRequest(
        String email,
        String password,
        String nickName
) {
}
