package com.example.userservice.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class InternalAuthInterceptor implements HandlerInterceptor {

    @Value("${internal.secret}")
    private String internalSecret;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {

        String serviceToken = request.getHeader("X-Service-Token");

        if (serviceToken == null || !internalSecret.equals(serviceToken)) {
            log.warn("내부 API 인증 실패: uri={}, token={}", request.getRequestURI(), serviceToken);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Forbidden\"}");
            return false;
        }

        return true;
    }
}
