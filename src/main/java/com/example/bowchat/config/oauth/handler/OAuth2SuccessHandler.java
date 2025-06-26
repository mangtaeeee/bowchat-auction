package com.example.bowchat.config.oauth.handler;

import com.example.bowchat.config.jwt.JwtProvider;
import com.example.bowchat.user.auth.repository.RefreshTokenRepository;
import com.example.bowchat.user.entity.PrincipalDetails;
import com.example.bowchat.user.entity.User;
import com.example.bowchat.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        log.info("1. OAuth2 로그인 성공");
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        log.info("2. 로그인한 사용자 정보: {}", principalDetails.getUser());
        User user = principalDetails.getUser();

        log.info("3. Access Token 및 Refresh Token 생성");
        String accessToken = jwtProvider.generateToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user.getEmail());

        log.info("4. Refresh Token 저장");
        refreshTokenRepository.save(
                user.getEmail(),
                refreshToken,
                jwtProvider.getRefreshTokenExpiration()
        );

        log.info("5. Access Token 및 Refresh Token 응답 헤더에 설정");
        response.addHeader("Set-Cookie", String.format(
                "refreshToken=%s; Path=/; HttpOnly; Max-Age=%d; SameSite=None; Secure",
                refreshToken,
                jwtProvider.getRefreshTokenExpiration() / 1000
        ));

        response.setHeader("Authorization", "Bearer " + accessToken);
        // ✅ 프론트엔드에게 Access Token 전달 (URL 또는 Header)
        String userAgent = request.getHeader("User-Agent");
        String redirectUrl;

        if (userAgent != null && userAgent.contains("Mozilla")) {
            // 브라우저 (타임리프)
            redirectUrl = "/chat?token=" + accessToken;
        } else {
            // API (React, Mobile 등)
            redirectUrl = "http://localhost:3000/oauth2/success?token=" + accessToken;
        }

        response.sendRedirect(redirectUrl);
    }

}
