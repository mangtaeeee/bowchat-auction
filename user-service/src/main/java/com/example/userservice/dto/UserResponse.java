package com.example.userservice.dto;

import com.example.bowchat.user.dto.UserInfo;

public record UserResponse(
        UserInfo userInfo
) {
}
