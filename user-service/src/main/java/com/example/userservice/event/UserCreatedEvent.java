package com.example.userservice.event;

import com.example.userservice.entity.User;
import lombok.Builder;

/**
 * 사용자 생성 이벤트
 * @param userId
 * @param email
 * @param nickName
 */
@Builder
public record UserCreatedEvent(
        Long userId,
        String email,
        String nickName
) {
    public static UserCreatedEvent of(User user) {
        return UserCreatedEvent.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickName(user.getNickname())
                .build();
   }
}