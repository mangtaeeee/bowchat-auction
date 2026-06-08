package com.example.userservice.auth.service;

import com.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Duration;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private TokenService tokenService;

    @Mock
    private KeycloakAuthService keycloakAuthService;

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
        when(refreshTokenService.findRefreshTokenByEmail(email)).thenReturn("refresh-token");

        authService.logout(accessToken, authenticatedJwt(email, Instant.now().plusSeconds(5)));

        // then: refresh token 삭제 후 access token이 blacklist에 등록돼야 한다.
        verify(keycloakAuthService).logout("refresh-token");
        verify(refreshTokenService).delete(email);
        verify(valueOperations).set(eq("blacklist:" + accessToken), eq("logout"), any(Duration.class));
    }

    @Test
    void logoutSkipsBlacklistWhenTokenIsAlreadyExpired() {
        // given: 이미 만료된 토큰이면 blacklist에 넣을 필요가 없다.
        String accessToken = "expired-access-token";
        String email = "user@test.com";
        when(refreshTokenService.findRefreshTokenByEmail(email)).thenReturn("refresh-token");

        authService.logout(accessToken, authenticatedJwt(email, Instant.now().minusSeconds(1)));

        // then: refresh token 정리는 하되 blacklist 저장은 건너뛴다.
        verify(keycloakAuthService).logout("refresh-token");
        verify(refreshTokenService).delete(email);
        verify(valueOperations, never()).set(eq("blacklist:" + accessToken), eq("logout"), any(Duration.class));
    }

    private org.springframework.security.core.Authentication authenticatedJwt(String email, Instant expiresAt) {
        Jwt jwt = new Jwt(
                "access-token",
                Instant.now().minusSeconds(10),
                expiresAt,
                java.util.Map.of("alg", "RS256"),
                java.util.Map.of(
                        "sub", email,
                        "preferred_username", email
                )
        );
        return new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt);
    }
}
