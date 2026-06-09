package com.example.userservice.auth.dto;

import com.example.userservice.dto.response.UserInfo;
public record AuthResponse(
        String accessToken,
        String refreshToken,
        Long refreshTokenExpiresIn,
        UserInfo userInfo
) {
    public static AuthResponse issued(String accessToken, String refreshToken, Long refreshTokenExpiresIn, UserInfo userInfo) {
        return new AuthResponse(accessToken, refreshToken, refreshTokenExpiresIn, userInfo);
    }

    public static AuthResponse refreshed(String accessToken, String refreshToken, Long refreshTokenExpiresIn) {
        return new AuthResponse(accessToken, refreshToken, refreshTokenExpiresIn, null);
    }
}
