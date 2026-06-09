package com.example.userservice.auth;

import com.example.userservice.auth.dto.AccessTokenResponse;
import com.example.userservice.auth.dto.AuthResponse;
import com.example.userservice.auth.dto.CurrentUserResponse;
import com.example.userservice.auth.service.AuthService;
import com.example.userservice.dto.request.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<AccessTokenResponse> refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.refreshAccessToken(request);
        String newAccessToken = authResponse.accessToken();

        response.setHeader(HttpHeaders.AUTHORIZATION, AuthConstants.BEARER_PREFIX + newAccessToken);
        response.addHeader(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(
                authResponse.refreshToken(),
                authResponse.refreshTokenExpiresIn()
        ).toString());

        return ResponseEntity.ok(new AccessTokenResponse(newAccessToken));
    }

    @GetMapping("/auth/me")
    public ResponseEntity<CurrentUserResponse> currentUser(Authentication authentication) {
        return ResponseEntity.ok(authService.currentUser(authentication));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken,
            Authentication authentication
    ) {
        String token = bearerToken.substring(AuthConstants.BEARER_PREFIX_LENGTH);
        authService.logout(token, authentication);
        return ResponseEntity.ok().build();
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken, Long expirationMillis) {
        long maxAge = expirationMillis == null ? 0L : expirationMillis;
        return ResponseCookie.from(AuthConstants.REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofMillis(maxAge))
                .sameSite("Strict")
                .build();
    }
}
