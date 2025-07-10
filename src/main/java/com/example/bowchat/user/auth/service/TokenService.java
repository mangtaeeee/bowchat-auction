package com.example.bowchat.user.auth.service;

import com.example.bowchat.user.auth.dto.AuthResponse;
import com.example.bowchat.user.auth.jwt.JwtProvider;
import com.example.bowchat.user.dto.UserInfo;
import com.example.bowchat.user.entity.User;
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

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse issueTokens(User user) {
        String accessToken = jwtProvider.generateToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user.getEmail());

        refreshTokenService.save(
                user.getEmail(),
                refreshToken,
                jwtProvider.getRefreshTokenExpiration()
        );


        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userInfo(UserInfo.of(user))
                .build();
    }

    public String extractRefreshToken(HttpServletRequest request) {
        return Optional.ofNullable(request.getCookies())
                .flatMap(cookies -> Arrays.stream(cookies)
                        .filter(c -> "refreshToken".equals(c.getName()))
                        .map(Cookie::getValue)
                        .findFirst())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 없습니다."));
    }

    public void validateRefreshToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            log.warn("리프레시 토큰 검증 실패");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
        }
    }

    public void verifyRefreshTokenInRedis(String email, String refreshToken) {
        String savedRefreshToken = refreshTokenService.findRefreshTokenByEmail(email);
        if (!refreshToken.equals(savedRefreshToken)) {
            log.warn("리프레시 토큰 불일치: 저장된={}, 요청된={}", savedRefreshToken, refreshToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 일치하지 않습니다.");
        }
    }


    public String issueNewAccessToken(User user) {
        String newAccessToken = jwtProvider.generateToken(user);
        log.info("새로운 액세스 토큰 발급 완료");
        return newAccessToken;
    }

}
