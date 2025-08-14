package com.example.bowchat.user.auth;

import com.example.bowchat.user.auth.dto.AuthResponse;
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

        response.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + body.accessToken());

        if (!isMobile(userAgent)) {
            // web에서 요청한 경우, Refresh Token을 쿠키에 설정
            ResponseCookie refreshCookie = createRefreshTokenCookie(body.refreshToken());
            response.getHeaders().add(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            return AuthResponse.builder()
                    .accessToken(body.accessToken())
                    .userInfo(body.userInfo())
                    .build();

        }
        return body;
    }

    private boolean isMobile(String userAgent) {
        return userAgent != null && (
                userAgent.contains("Android") || userAgent.contains("iPhone") || userAgent.contains("iPad")
        );
    }

    private static ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("Strict")
                .build();
    }
}
