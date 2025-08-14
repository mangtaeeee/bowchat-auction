package com.example.bowchat.user.dto;

public record LoginRequest(
        String email,
        String password
) {
}
