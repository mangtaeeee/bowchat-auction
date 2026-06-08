package com.example.chatservice.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtDecoder jwtDecoder;

    public boolean validateToken(String token) {
        try {
            jwtDecoder.decode(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        Jwt jwt = jwtDecoder.decode(token);
        Object claim = jwt.getClaim(AuthConstants.JWT_CLAIM_USER_ID);
        if (claim instanceof Number number) {
            return number.longValue();
        }
        if (claim instanceof String value) {
            return Long.parseLong(value);
        }
        throw new IllegalStateException("Keycloak access token에 userId claim이 없습니다.");
    }
}
