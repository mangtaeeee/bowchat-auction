package com.example.userservice.dto.request;

import com.example.userservice.entity.User;
import lombok.Builder;

@Builder
public record UserSnapshot(
        Long userId,
        String email,
        String nickname
) {
    public static UserSnapshot of(User user) {
        return UserSnapshot.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }
}
