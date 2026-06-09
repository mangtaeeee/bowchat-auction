package com.example.userservice.dto.request;

public record SignUpRequest(
        String email,
        String password,
        String nickName
) {
}

