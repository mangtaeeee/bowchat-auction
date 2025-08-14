package com.example.bowchat.user.auth.oauth.handler;

import com.example.bowchat.user.auth.dto.AuthResponse;
import com.example.bowchat.user.auth.service.TokenService;
import com.example.bowchat.user.entity.PrincipalDetails;
import com.example.bowchat.user.entity.User;
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

    private final TokenService tokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        log.info(" OAuth2 로그인 성공");
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        User user = principalDetails.getUser();

        AuthResponse authResponse = tokenService.issueTokens(user);

        // Request에 AuthResponse 임시 저장 (어드바이스에서 후처리)
        request.setAttribute("authResponse", authResponse);
        String accessToken = authResponse.accessToken();

        // 브라우저인지 모바일인지에 따라 리다이렉트 URL 지정
        String userAgent = request.getHeader("User-Agent");
        String redirectUrl = userAgent != null && userAgent.contains("Mozilla")
                ? "/view/product?token=" + accessToken  // 웹
                : "http://localhost:3000/oauth2/success"; // 모바일/React

        response.sendRedirect(redirectUrl);
    }
}
