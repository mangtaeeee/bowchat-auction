package com.example.bowchat.user.auth.dto;

import lombok.Builder;

@Builder
public record AuthResponse(
        String accessToken
) {
}
