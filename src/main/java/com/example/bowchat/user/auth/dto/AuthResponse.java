package com.example.bowchat.user.auth.dto;

import com.example.bowchat.user.dto.UserInfo;
import lombok.Builder;

@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserInfo userInfo
) {
}
