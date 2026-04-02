package com.example.userservice.dto.response;


public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long accessTokenExpiresIn,
        Long refreshTokenExpiresIn
) {
}
