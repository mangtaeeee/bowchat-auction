package com.example.userservice.auth;

import com.example.userservice.entity.Role;

public record UserPrincipal(
        Long userId,
        String email,
        String nickname,
        Role role
) {
}
