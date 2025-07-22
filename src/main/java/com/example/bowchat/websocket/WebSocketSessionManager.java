package com.example.bowchat.websocket;

import com.example.bowchat.kafka.ChatEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WebSocketSessionManager {
    private final Map<Long, List<WebSocketSession>> sessionsByRoom = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public WebSocketSessionManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void addSession(Long roomId, WebSocketSession session) {
        sessionsByRoom.computeIfAbsent(roomId, k -> new ArrayList<>()).add(session);
    }

    public void removeSession(Long roomId, WebSocketSession session) {
        List<WebSocketSession> sessions = sessionsByRoom.get(roomId);
        if (sessions != null) {
            sessions.remove(session);
        }
    }

    public void broadcast(Long roomId, ChatEvent chatEvent) {
        List<WebSocketSession> sessions = sessionsByRoom.get(roomId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                try {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(chatEvent)));
                } catch (IOException e) {
                    log.error("메시지 전송 실패", e);
                }
            }
        }
    }

}