package com.example.userservice.auth.dto;

import com.example.userservice.dto.response.UserInfo;
import lombok.Builder;

@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserInfo userInfo
) {
}
