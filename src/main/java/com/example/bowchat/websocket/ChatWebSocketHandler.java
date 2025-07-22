package com.example.bowchat.websocket;

import com.example.bowchat.kafka.ChatEvent;
import com.example.bowchat.kafka.ChatProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private final ChatProducer kafkaProducer;
    private final ObjectMapper objectMapper;


    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long roomId = extractRoomId(session);
        sessionManager.addSession(roomId, session);
        log.info("WebSocket 연결됨 - roomId={}, sessionId={}", roomId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long roomId = extractRoomId(session);
        sessionManager.removeSession(roomId, session);
        log.info("WebSocket 연결 종료 - roomId={}, sessionId={}", roomId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            Long roomId = extractRoomId(session);
            log.info("WebSocket 수신 메시지: {}", message.getPayload());

            // 클라이언트 메시지를 ChatEvent로 매핑
            ChatEvent chatEvent = objectMapper.readValue(message.getPayload(), ChatEvent.class);

            ChatEvent enrichedEvent = ChatEvent.enrichChatEvent(roomId, chatEvent);

            kafkaProducer.send(enrichedEvent);
            log.info("Kafka에 메시지 전송 완료: {}", enrichedEvent);

        } catch (Exception e) {
            log.error("WebSocket 메시지 처리 중 오류 발생: {}", e.getMessage());
        }
    }

    private Long extractRoomId(WebSocketSession session) {
        String query = Objects.requireNonNull(session.getUri()).getQuery(); // e.g., token=xxx&roomId=1
        Map<String, String> params = Arrays.stream(query.split("&"))
                .map(s -> s.split("="))
                .collect(Collectors.toMap(a -> a[0], a -> a[1]));
        return Long.parseLong(params.get("roomId"));
    }
}