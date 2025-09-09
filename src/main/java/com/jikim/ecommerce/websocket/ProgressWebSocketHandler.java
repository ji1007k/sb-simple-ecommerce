package com.jikim.ecommerce.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jikim.ecommerce.dto.DownloadProgress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProgressWebSocketHandler extends TextWebSocketHandler {
    
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        log.info("WebSocket connection established: {}", sessionId);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        log.info("WebSocket connection closed: {}", sessionId);
    }
    
    /**
     * 특정 세션에 진행률 전송
     */
    public void sendProgress(String sessionId, DownloadProgress progress) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                String message = objectMapper.writeValueAsString(progress);
                session.sendMessage(new TextMessage(message));
                log.debug("Progress sent to session {}: {}%", sessionId, progress.getProgressPercentage());
            } catch (IOException e) {
                log.error("Failed to send progress to session: {}", sessionId, e);
                sessions.remove(sessionId); // 실패한 세션 제거
            }
        }
    }
    
    /**
     * 모든 활성 세션에 메시지 브로드캐스트
     */
    public void broadcastProgress(DownloadProgress progress) {
        sessions.values().parallelStream()
                .filter(WebSocketSession::isOpen)
                .forEach(session -> {
                    try {
                        String message = objectMapper.writeValueAsString(progress);
                        session.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        log.error("Failed to broadcast to session: {}", session.getId(), e);
                        sessions.remove(session.getId());
                    }
                });
    }
    
    /**
     * 활성 세션 수 조회
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
}
