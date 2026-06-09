package com.example.userservice.dto.response;

import com.example.userservice.entity.ProviderType;
import com.example.userservice.entity.User;
public record UserInfo(
        Long id,
        String email,
        String nickname,
        ProviderType provider
) {
    public static UserInfo of(User user) {
        return new UserInfo(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProvider()
        );
    }
}
