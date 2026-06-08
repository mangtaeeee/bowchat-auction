package com.example.userservice.auth.service;

import com.example.userservice.auth.AuthConstants;
import com.example.userservice.auth.dto.AuthResponse;
import com.example.userservice.auth.dto.KeycloakTokenResponse;
import com.example.userservice.dto.response.UserInfo;
import com.example.userservice.entity.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private static final long REFRESH_TOKEN_TTL_FALLBACK_MILLIS = 1_209_600_000L;

    private final RefreshTokenService refreshTokenService;

    public AuthResponse issueTokens(User user, KeycloakTokenResponse keycloakTokenResponse) {
        String accessToken = keycloakTokenResponse.accessToken();
        String refreshToken = keycloakTokenResponse.refreshToken();
        long refreshTokenExpiration = resolveRefreshTokenExpiration(keycloakTokenResponse);
        refreshTokenService.save(user.getEmail(), refreshToken, refreshTokenExpiration);

        return AuthResponse.issued(accessToken, refreshToken, refreshTokenExpiration, UserInfo.of(user));
    }

    public String extractRefreshToken(HttpServletRequest request) {
        return Optional.ofNullable(request.getCookies())
                .flatMap(cookies -> Arrays.stream(cookies)
                        .filter(c -> AuthConstants.REFRESH_TOKEN_COOKIE_NAME.equals(c.getName()))
                        .map(Cookie::getValue)
                        .findFirst())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 없습니다."));
    }

    public void verifyRefreshTokenInRedis(String email, String refreshToken) {
        String savedRefreshToken = refreshTokenService.findRefreshTokenByEmail(email);
        if (!refreshToken.equals(savedRefreshToken)) {
            log.warn("리프레시 토큰 불일치: 저장된={}, 요청된={}", savedRefreshToken, refreshToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 일치하지 않습니다.");
        }
    }

    public long resolveRefreshTokenExpiration(KeycloakTokenResponse keycloakTokenResponse) {
        if (keycloakTokenResponse.refreshExpiresIn() == null) {
            return REFRESH_TOKEN_TTL_FALLBACK_MILLIS;
        }
        return keycloakTokenResponse.refreshExpiresIn() * 1000L;
    }
}
