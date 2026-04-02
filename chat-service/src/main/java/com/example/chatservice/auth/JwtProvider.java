package com.example.chatservice.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties jwtProperties;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        Long userId     = claims.get("userId", Long.class);
        String email    = claims.getSubject();
        String nickname = claims.get("nickname", String.class);
        String role     = claims.get("role", String.class);

        UserPrincipal principal = new UserPrincipal(userId, email, nickname, role);
        return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // WebSocket HandshakeInterceptor에서 토큰 추출 시 사용
    public Long getUserId(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
