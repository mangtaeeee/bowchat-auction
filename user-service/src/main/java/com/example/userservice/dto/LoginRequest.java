package com.example.userservice.dto;

public record LoginRequest(
        String email,
        String password
) {
}
