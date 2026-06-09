package com.example.userservice.auth;

import com.example.userservice.auth.dto.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.time.Duration;

@ControllerAdvice
@RequiredArgsConstructor
public class TokenResponseAdvice implements ResponseBodyAdvice<AuthResponse> {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return AuthResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public AuthResponse beforeBodyWrite(AuthResponse body,
                                        MethodParameter returnType,
                                        MediaType selectedContentType,
                                        Class selectedConverterType,
                                        ServerHttpRequest request,
                                        ServerHttpResponse response) {

        String userAgent = request.getHeaders().getFirst("User-Agent");

        response.getHeaders().set(HttpHeaders.AUTHORIZATION, AuthConstants.BEARER_PREFIX + body.accessToken());

        if (!isMobile(userAgent)) {
            // web에서 요청한 경우, Refresh Token을 쿠키에 설정
            ResponseCookie refreshCookie = createRefreshTokenCookie(body.refreshToken(), body.refreshTokenExpiresIn());
            response.getHeaders().add(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            return new AuthResponse(
                    body.accessToken(),
                    null,
                    body.refreshTokenExpiresIn(),
                    body.userInfo()
            );

        }
        return body;
    }

    private boolean isMobile(String userAgent) {
        return userAgent != null && (
                userAgent.contains("Android") || userAgent.contains("iPhone") || userAgent.contains("iPad")
        );
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
