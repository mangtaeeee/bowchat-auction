package com.example.bowchat.user.entity;


import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Arrays;

@Getter
public enum Role implements GrantedAuthority {
    USER, ADMIN;

    @Override
    public String getAuthority() {
        return this.name();
    }

    public static boolean contains(String userRole) {
        return Arrays.stream(Role.values()).anyMatch((e) -> e.name().equals(userRole));
    }

}
