package com.example.userservice.auth.service;

import com.example.userservice.auth.dto.AuthResponse;
import com.example.userservice.auth.jwt.JwtProvider;
import com.example.userservice.dto.request.LoginRequest;
import com.example.userservice.entity.PrincipalDetails;
import com.example.userservice.entity.User;
import com.example.userservice.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {


    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final TokenService tokenService;
    private final RedisTemplate<String, Object> redisTemplate;

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
        String refreshToken = tokenService.extractRefreshToken(request);

        tokenService.validateRefreshToken(refreshToken);

        String email = jwtProvider.getEmailFromToken(refreshToken);

        tokenService.verifyRefreshTokenInRedis(email, refreshToken);

        User user = loadUser(email);

        return tokenService.issueNewAccessToken(user);
    }


    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    public void logout(String token, String email) {
        // 1. Redis에서 Refresh Token 삭제
        refreshTokenService.delete(email);

        // 2. Access Token 블랙리스트 등록 (남은 만료시간만큼 TTL)
        long expiration = jwtProvider.getExpiration(token);
        if (expiration > 0) {
            redisTemplate.opsForValue().set(
                    "blacklist:" + token,
                    "logout",
                    Duration.ofMillis(expiration)
            );
        }
    }
}
