package com.example.bowchat.user.auth.service;

import com.example.bowchat.user.auth.dto.AuthResponse;
import com.example.bowchat.user.auth.jwt.JwtProvider;
import com.example.bowchat.user.dto.LoginRequest;
import com.example.bowchat.user.entity.PrincipalDetails;
import com.example.bowchat.user.entity.User;
import com.example.bowchat.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {


    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final TokenService tokenService;

    public AuthResponse login(LoginRequest loginRequest) {
        log.info("CustomAuthenticationProvider로 인증 시도");

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email(),
                        loginRequest.password()
                )
        );

        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        User user = principalDetails.getUser();

        return tokenService.issueTokens(user);
    }

    public String refreshAccessToken(HttpServletRequest request) {
        // 1. 쿠키에서 리프레시 토큰 추출
        String refreshToken = getRefreshTokenFromCookies(request);
        log.info("리프레시 토큰 추출: {}", refreshToken);

        // 2. 리프레시 토큰 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            log.warn("리프레시 토큰 검증 실패");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
        }

        // 3. 이메일 추출 및 Redis 조회
        String email = jwtProvider.getEmailFromToken(refreshToken);
        String savedRefreshToken = refreshTokenService.findRefreshTokenByEmail(email);

        if (!refreshToken.equals(savedRefreshToken)) {
            log.warn("리프레시 토큰 불일치: 저장된={}, 요청된={}", savedRefreshToken, refreshToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 일치하지 않습니다.");
        }

        // 4. 사용자 조회 및 새 액세스 토큰 발급
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        String newAccessToken = jwtProvider.generateToken(user);
        log.info("새로운 액세스 토큰 발급 완료");

        return newAccessToken;
    }


    public static String getRefreshTokenFromCookies(HttpServletRequest request) {
        return Optional.ofNullable(request.getCookies())
                .flatMap(cookies -> Arrays.stream(cookies)
                        .filter(c -> "refreshToken".equals(c.getName()))
                        .map(Cookie::getValue)
                        .findFirst())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 없습니다."));
    }
}
