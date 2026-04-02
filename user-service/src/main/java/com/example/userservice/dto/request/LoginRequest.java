package com.example.userservice.dto.request;

public record LoginRequest(
        String email,
        String password
) {
}
