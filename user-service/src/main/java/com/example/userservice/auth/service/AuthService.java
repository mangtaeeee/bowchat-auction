package com.example.userservice.auth.service;

import com.example.userservice.auth.AuthConstants;
import com.example.userservice.auth.UserJwtAuthenticationToken;
import com.example.userservice.auth.UserPrincipal;
import com.example.userservice.auth.dto.CurrentUserResponse;
import com.example.userservice.auth.dto.AuthResponse;
import com.example.userservice.auth.dto.KeycloakTokenResponse;
import com.example.userservice.config.AuthFeatureProperties;
import com.example.userservice.dto.request.LoginRequest;
import com.example.userservice.entity.PrincipalDetails;
import com.example.userservice.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final TokenService tokenService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KeycloakAuthService keycloakAuthService;
    private final AuthFeatureProperties authFeatureProperties;

    public AuthResponse login(LoginRequest loginRequest) {
        if (!authFeatureProperties.isLocalPasswordLoginEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "로컬 비밀번호 로그인은 비활성화되었습니다. Keycloak/OAuth2 로그인 경로를 사용하세요."
            );
        }

        log.info("CustomAuthenticationProvider로 인증 시도");

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email(),
                        loginRequest.password()
                )
        );
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        User user = principalDetails.getUser();

        // 로컬 사용자 프로필과 Keycloak 인증 원장을 같은 계정으로 맞춘다.
        keycloakAuthService.syncLocalUser(user, loginRequest.password());
        KeycloakTokenResponse keycloakTokenResponse = keycloakAuthService.login(loginRequest.email(), loginRequest.password());

        return tokenService.issueTokens(user, keycloakTokenResponse);
    }

    public AuthResponse refreshAccessToken(HttpServletRequest request) {
        String refreshToken = tokenService.extractRefreshToken(request);
        String email = refreshTokenService.findEmailByRefreshToken(refreshToken);

        tokenService.verifyRefreshTokenInRedis(email, refreshToken);
        KeycloakTokenResponse keycloakTokenResponse = keycloakAuthService.refresh(refreshToken);
        long refreshTokenExpiration = tokenService.resolveRefreshTokenExpiration(keycloakTokenResponse);
        refreshTokenService.replace(
                email,
                refreshToken,
                keycloakTokenResponse.refreshToken(),
                refreshTokenExpiration
        );

        return AuthResponse.refreshed(
                keycloakTokenResponse.accessToken(),
                keycloakTokenResponse.refreshToken(),
                refreshTokenExpiration
        );
    }

    public CurrentUserResponse currentUser(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (authentication instanceof UserJwtAuthenticationToken userAuthentication) {
            return CurrentUserResponse.from(userAuthentication.getPrincipal());
        }
        if (principal instanceof PrincipalDetails principalDetails) {
            return CurrentUserResponse.from(principalDetails.getUser());
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 사용자 정보를 해석할 수 없습니다.");
    }

    public void logout(String token, Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        String refreshToken = refreshTokenService.findRefreshTokenByEmail(email);
        keycloakAuthService.logout(refreshToken);
        refreshTokenService.delete(email);

        // Keycloak access token도 서비스 공통 Redis blacklist에 올려 즉시 무효화한다.
        long expiration = resolveExpirationMillis(authentication);
        if (expiration > 0) {
            redisTemplate.opsForValue().set(
                    AuthConstants.BLACKLIST_PREFIX + token,
                    AuthConstants.LOGOUT_MARKER,
                    Duration.ofMillis(expiration)
            );
        }
    }

    private String resolveAuthenticatedEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (authentication instanceof UserJwtAuthenticationToken userAuthentication) {
            return userAuthentication.getPrincipal().email();
        }
        if (principal instanceof Jwt jwt) {
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (preferredUsername != null && !preferredUsername.isBlank()) {
                return preferredUsername;
            }

            String email = jwt.getClaimAsString("email");
            if (email != null && !email.isBlank()) {
                return email;
            }
        }

        return authentication.getName();
    }

    private long resolveExpirationMillis(Authentication authentication) {
        if (authentication instanceof UserJwtAuthenticationToken userAuthentication) {
            Instant expiresAt = userAuthentication.getJwt().getExpiresAt();
            if (expiresAt == null) {
                return 0L;
            }
            return expiresAt.toEpochMilli() - System.currentTimeMillis();
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            return 0L;
        }

        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt == null) {
            return 0L;
        }

        return expiresAt.toEpochMilli() - System.currentTimeMillis();
    }
}
