package com.example.userservice.auth.oauth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final long REFRESH_TOKEN_TTL_FALLBACK_MILLIS = 1_209_600_000L;

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final com.example.userservice.auth.service.RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        log.info("Keycloak 브로커 OAuth2 로그인 성공");
        OAuth2AuthenticationToken oauth2Authentication = (OAuth2AuthenticationToken) authentication;
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                oauth2Authentication.getAuthorizedClientRegistrationId(),
                oauth2Authentication.getName()
        );

        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            throw new IllegalStateException("Keycloak OAuth2 로그인 후 access token을 찾을 수 없습니다.");
        }

        OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();
        if (refreshToken != null) {
            long refreshTokenExpiration = resolveRefreshTokenExpiration(refreshToken);
            refreshTokenService.save(oidcUser.getEmail(), refreshToken.getTokenValue(), refreshTokenExpiration);
            response.addHeader(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(
                    refreshToken.getTokenValue(),
                    refreshTokenExpiration
            ).toString());
        }

        // access token은 URL에 노출하지 않고, 클라이언트가 refresh cookie로 후속 교환하도록 한다.
        String userAgent = request.getHeader("User-Agent");
        String redirectUrl = userAgent != null && userAgent.contains("Mozilla")
                ? "/view/product"
                : "http://localhost:3000/oauth2/success";

        response.sendRedirect(redirectUrl);
    }

    private long resolveRefreshTokenExpiration(OAuth2RefreshToken refreshToken) {
        Instant expiresAt = refreshToken.getExpiresAt();
        if (expiresAt == null) {
            return REFRESH_TOKEN_TTL_FALLBACK_MILLIS;
        }
        return Math.max(0L, expiresAt.toEpochMilli() - System.currentTimeMillis());
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken, long expirationMillis) {
        return ResponseCookie.from(com.example.userservice.auth.AuthConstants.REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofMillis(expirationMillis))
                .sameSite("Strict")
                .build();
    }
}
