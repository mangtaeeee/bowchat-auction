package com.example.userservice.event;

import com.example.userservice.entity.User;
import lombok.Builder;

@Builder
public record UserCreatedEvent(
        /*
    * UserCreatedEvent는 사용자 생성 이벤트를 나타내는 클래스입니다. 이 이벤트는 사용자가 성공적으로 생성되었을 때 발행됩니다.
         */
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