package com.example.bowchat.user.auth.jwt;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request,
                                   @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler,
                                   @NonNull Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String query = servletRequest.getServletRequest().getQueryString(); // e.g., "token=xxx.yyy.zzz"

            if (query != null && query.startsWith("token=")) {
                String token = query.substring(6); // 토큰만 추출
                try {
                    Principal principal = jwtProvider.getAuthentication(token);
                    attributes.put("user", principal); // WebSocketSession에서 사용할 수 있게 저장
                    return true;
                } catch (Exception e) {
                    log.info("WebSocket Handshake 실패: {}", e.getMessage());
                    return false;
                }
            }
        }

        return false; // 토큰 없으면 연결 거부
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request,
                               @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler, Exception exception) {
    }
}
