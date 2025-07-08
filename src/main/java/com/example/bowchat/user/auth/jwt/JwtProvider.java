package com.example.bowchat.user.auth.jwt;

import com.example.bowchat.user.entity.PrincipalDetails;
import com.example.bowchat.user.entity.ProviderType;
import com.example.bowchat.user.entity.User;
import com.example.bowchat.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.security.Key;
import java.util.Date;

@RequiredArgsConstructor
@Component
public class JwtProvider {

    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes());
    }

    // 로컬 로그인 시 Access Token 생성
    public String generateToken(Authentication authentication) {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        return Jwts.builder()
                .setSubject(principalDetails.getUsername())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    // SNS 로그인 시 Access Token 생성
    public String generateToken(User user) {
        JwtBuilder builder= Jwts.builder()
                .setSubject(user.getEmail())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiration()))
                .signWith(getSigningKey());

        if (user.getProvider() != ProviderType.LOCAL) {
            builder.claim("provider", user.getProvider().name());
        }

        return builder.compact();
    }

    // Refresh Token 생성
    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshTokenExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    public long getRefreshTokenExpiration() {
        return jwtProperties.getRefreshTokenExpiration();
    }


    //토큰에서 인증정보 꺼내기
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        String email = claims.getSubject();
        String providerName = claims.get("provider", String.class);

        ProviderType provider = (providerName == null)
                ? ProviderType.LOCAL
                : ProviderType.valueOf(providerName);

        User user = userRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        PrincipalDetails principal = new PrincipalDetails(user);

        return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
    }

    // 토큰 검증
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
