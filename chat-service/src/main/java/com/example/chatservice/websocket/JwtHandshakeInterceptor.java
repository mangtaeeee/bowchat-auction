package com.example.chatservice.websocket;

import com.example.chatservice.auth.JwtProvider;
import com.example.chatservice.chatroom.service.ChatRoomAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatRoomAccessService chatRoomAccessService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        String token = resolveToken(request);
        if (token == null || !jwtProvider.validateToken(token)) {
            log.warn("WebSocket 핸드셰이크 실패: 유효하지 않은 토큰");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        Long userId = jwtProvider.getUserId(token);
        if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token))) {
            log.warn("WebSocket 핸드셰이크 실패: 블랙리스트 토큰 userId={}", userId);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        Long roomId = extractRoomId(request);
        if (roomId == null || !chatRoomAccessService.isActiveParticipant(roomId, userId)) {
            log.warn("WebSocket 핸드셰이크 실패: 방 참여 권한 없음 roomId={}, userId={}", roomId, userId);
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        attributes.put("userId", userId);  // WebSocketSession에 userId 저장
        attributes.put("roomId", roomId);
        log.debug("WebSocket 핸드셰이크 성공: userId={}", userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private Long extractRoomId(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        if (path == null) {
            return null;
        }

        String[] parts = path.split("/");
        try {
            return Long.parseLong(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String resolveToken(ServerHttpRequest request) {
        // 쿼리 파라미터에서 토큰 추출 (?token=xxx)
        String query = request.getURI().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=");
                if (kv.length == 2 && "token".equals(kv[0])) {
                    return kv[1];
                }
            }
        }
        // Authorization 헤더에서도 추출 시도
        String auth = request.getHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }
}
