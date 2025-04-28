package com.example.bowchat.config.jwt;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class JwtProperties {

    @Value("${jwt.secret}")
    private String secretKey;

    private final long accessTokenExpiration = 1000 * 60 * 60 * 5; // 5시간
}
