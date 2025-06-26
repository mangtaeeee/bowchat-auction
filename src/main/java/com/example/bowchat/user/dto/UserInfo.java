package com.example.bowchat.user.dto;

import com.example.bowchat.user.entity.ProviderType;
import com.example.bowchat.user.entity.User;
import lombok.Builder;

@Builder
public record UserInfo(
        Long id,
        String email,
        String nickname,
        ProviderType provider
) {
    public static UserInfo of(User user) {
        return UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .provider(user.getProvider())
                .build();
    }
}
