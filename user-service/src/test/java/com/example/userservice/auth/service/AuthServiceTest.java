package com.example.userservice.auth.service;

import com.example.userservice.auth.jwt.JwtProvider;
import com.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // logout은 JWT 만료시간과 Redis blacklist 기록 여부가 핵심이다.
    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private TokenService tokenService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AuthService authService;

    @Test
    void logoutDeletesRefreshTokenAndBlacklistsAccessToken() {
        // given: 아직 만료되지 않은 access token을 준비한다.
        String accessToken = "access-token";
        String email = "user@test.com";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtProvider.getExpiration(accessToken)).thenReturn(5_000L);

        authService.logout(accessToken, email);

        // then: refresh token 삭제 후 access token이 blacklist에 등록돼야 한다.
        verify(refreshTokenService).delete(email);
        verify(valueOperations).set(eq("blacklist:" + accessToken), eq("logout"), any(Duration.class));
    }

    @Test
    void logoutSkipsBlacklistWhenTokenIsAlreadyExpired() {
        // given: 이미 만료된 토큰이면 blacklist에 넣을 필요가 없다.
        String accessToken = "expired-access-token";
        String email = "user@test.com";
        when(jwtProvider.getExpiration(accessToken)).thenReturn(0L);

        authService.logout(accessToken, email);

        // then: refresh token 정리는 하되 blacklist 저장은 건너뛴다.
        verify(refreshTokenService).delete(email);
        verify(valueOperations, never()).set(eq("blacklist:" + accessToken), eq("logout"), any(Duration.class));
    }
}
