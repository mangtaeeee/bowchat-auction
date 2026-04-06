package com.example.chatservice.websocket;

import com.example.bowchat.kafkastarter.event.EventMessage;
import com.example.bowchat.kafkastarter.event.MessageType;
import com.example.bowchat.kafkastarter.producer.ChatProducer;
import com.example.chatservice.chatroom.service.ChatRoomAccessService;
import com.example.chatservice.user.service.UserQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatProducer chatProducer;
    private final UserQueryService userQueryService;
    private final ChatRoomAccessService chatRoomAccessService;
    private final ObjectMapper objectMapper;

    // roomId → 해당 방의 WebSocketSession 목록
    private final Map<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long roomId = extractRoomId(session);
        Long userId = (Long) session.getAttributes().get("userId");
        log.info("WebSocket 연결: roomId={}, userId={}", roomId, userId);
        roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long roomId = extractRoomId(session);
        Long userId = (Long) session.getAttributes().get("userId");
        String payload = message.getPayload();

        if (!chatRoomAccessService.isActiveParticipant(roomId, userId)) {
            log.warn("비인가 WebSocket 메시지 차단: roomId={}, userId={}", roomId, userId);
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        log.debug("메시지 수신: roomId={}, userId={}, payload={}", roomId, userId, payload);

        String nickname = userQueryService.getUser(userId).getNickname();

        // Kafka로 발행 → SaveConsumer(MongoDB 저장) + BroadcastConsumer(WebSocket 브로드캐스트)
        EventMessage event = EventMessage.builder()
                .roomId(roomId)
                .senderId(userId)
                .senderName(nickname)
                .topicName(MessageType.CHAT.getTopicName())
                .messageType(MessageType.CHAT.name())
                .content(payload)
                .timestamp(Instant.now().toEpochMilli())
                .build();

        chatProducer.send(event);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long roomId = extractRoomId(session);
        Long userId = (Long) session.getAttributes().get("userId");
        log.info("WebSocket 연결 종료: roomId={}, userId={}", roomId, userId);

        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                roomSessions.remove(roomId);
            }
        }
    }

    // 특정 방의 모든 세션에 메시지 브로드캐스트
    public void broadcast(Long roomId, String message) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null || sessions.isEmpty()) return;

        sessions.forEach(session -> {
            try {
                if (!session.isOpen()) {
                    return;
                }

                Long userId = (Long) session.getAttributes().get("userId");
                if (!chatRoomAccessService.isActiveParticipant(roomId, userId)) {
                    session.close(CloseStatus.POLICY_VIOLATION);
                    return;
                }

                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (Exception e) {
                log.error("브로드캐스트 실패: roomId={}, error={}", roomId, e.getMessage());
            }
        });
    }

    private Long extractRoomId(WebSocketSession session) {
        Object roomId = session.getAttributes().get("roomId");
        if (roomId instanceof Long value) {
            return value;
        }

        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        return Long.parseLong(parts[parts.length - 1]);
    }
}
