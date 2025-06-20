package com.example.bowchat.config.oauth.handler;

import com.example.bowchat.config.jwt.JwtProvider;
import com.example.bowchat.user.entity.PrincipalDetails;
import com.example.bowchat.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();

        String jwtToken = jwtProvider.generateToken(principalDetails.getUser());

        // 리다이렉트 시 JWT를 쿼리 파라미터로 전달
        String userAgent = request.getHeader("User-Agent");
        String redirectUrl;

        if (userAgent != null && userAgent.contains("Mozilla")) {
            // 브라우저 요청: 타임리프 페이지로 이동
            response.setHeader("Authorization", "Bearer " + jwtToken); // 참고: 필요 시
            request.getSession().setAttribute("jwtToken", jwtToken);   // 또는 세션에 저장
            redirectUrl = "/chat?token=" + jwtToken;
        } else {
            // API 클라이언트(React 등) 요청
            redirectUrl = "http://localhost:3000/oauth2/success?token=" + jwtToken;
        }

        response.sendRedirect(redirectUrl);
    }

}
