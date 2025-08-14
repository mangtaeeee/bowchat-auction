package com.example.bowchat.user.dto;

import lombok.Getter;


public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long accessTokenExpiresIn,
        Long refreshTokenExpiresIn
) {
}
