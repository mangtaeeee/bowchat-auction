package com.example.userservice.auth.service;

import com.example.userservice.auth.UserJwtAuthenticationToken;
import com.example.userservice.auth.UserPrincipal;
import com.example.userservice.entity.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
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
        // given:
        // 일반 사용자 JWT가 이미 SecurityContext에 들어와 있다고 가정한다.
        // 로그아웃 시에는 이 JWT의 남은 만료 시간을 읽어서 Redis blacklist TTL로 써야 한다.
        String accessToken = "access-token";
        String email = "user@test.com";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(refreshTokenService.findRefreshTokenByEmail(email)).thenReturn("refresh-token");

        authService.logout(accessToken, authenticatedUserJwt(email, Instant.now().plusSeconds(5)));

        // then:
        // 1) Keycloak logout 호출
        // 2) 저장된 refresh token 삭제
        // 3) 현재 access token을 Redis blacklist에 등록
        verify(keycloakAuthService).logout("refresh-token");
        verify(refreshTokenService).delete(email);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.eq("blacklist:" + accessToken),
                org.mockito.ArgumentMatchers.eq("logout"),
                ttlCaptor.capture()
        );
        assertThat(ttlCaptor.getValue()).isPositive();
    }

    @Test
    void logoutSkipsBlacklistWhenTokenIsAlreadyExpired() {
        // given:
        // 이미 만료된 JWT는 더 이상 서버가 막을 필요가 없으므로 blacklist에 넣지 않는다.
        String accessToken = "expired-access-token";
        String email = "user@test.com";
        when(refreshTokenService.findRefreshTokenByEmail(email)).thenReturn("refresh-token");

        authService.logout(accessToken, authenticatedUserJwt(email, Instant.now().minusSeconds(1)));

        // then:
        // 세션 정리는 수행하지만 Redis blacklist 쓰기는 생략한다.
        verify(keycloakAuthService).logout("refresh-token");
        verify(refreshTokenService).delete(email);
        verifyNoInteractions(valueOperations);
    }

    private org.springframework.security.core.Authentication authenticatedUserJwt(String email, Instant expiresAt) {
        // 테스트용 사용자 인증 객체.
        // 실제 운영에서는 UserJwtAuthenticationConfig가 Keycloak JWT를 이 형태로 변환한다.
        return new UserJwtAuthenticationToken(
                new org.springframework.security.oauth2.jwt.Jwt(
                        "access-token",
                        Instant.now().minusSeconds(10),
                        expiresAt,
                        java.util.Map.of("alg", "RS256"),
                        java.util.Map.of(
                                "sub", email,
                                "email", email,
                                "userId", "1",
                                "nickname", "tester",
                                "role", "USER"
                        )
                ),
                new UserPrincipal(1L, email, "tester", Role.USER),
                List.of(Role.USER)
        );
    }
}
