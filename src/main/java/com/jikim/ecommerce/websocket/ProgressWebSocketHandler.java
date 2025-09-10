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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProgressWebSocketHandler extends TextWebSocketHandler {
    
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    // HTTP 세션 ID -> 웹소켓 세션 ID 매핑
    private final ConcurrentHashMap<String, String> httpToWebSocketSessionMapping = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String webSocketSessionId = session.getId();
        sessions.put(webSocketSessionId, session);
        
        // URL 쿼리 파라미터에서 HTTP 세션 ID 추출
        String httpSessionId = extractHttpSessionId(session);
        log.info("WebSocket connection established - WS ID: {}, URI: {}, Query: {}", 
            webSocketSessionId, session.getUri(), session.getUri().getQuery());
        
        if (httpSessionId != null) {
            httpToWebSocketSessionMapping.put(httpSessionId, webSocketSessionId);
            log.info("✅ Session mapping created: HTTP={} -> WS={}", httpSessionId, webSocketSessionId);
        } else {
            log.warn("❌ Could not extract HTTP session ID from URI: {}", session.getUri());
        }
        
        // 매핑 상태 출력
        debugSessionMappings();
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String webSocketSessionId = session.getId();
        sessions.remove(webSocketSessionId);
        
        // 역방향 매핑도 제거
        httpToWebSocketSessionMapping.entrySet().removeIf(entry -> 
            entry.getValue().equals(webSocketSessionId));
        
        log.info("WebSocket connection closed: {}", webSocketSessionId);
    }
    
    /**
     * 특정 세션에 진행률 전송 - HTTP 세션 ID 기반
     */
    public void sendProgress(String httpSessionId, DownloadProgress progress) {
        String webSocketSessionId = httpToWebSocketSessionMapping.get(httpSessionId);
        if (webSocketSessionId != null) {
            WebSocketSession session = sessions.get(webSocketSessionId);
            if (session != null && session.isOpen()) {
                try {
                    String message = objectMapper.writeValueAsString(progress);
                    session.sendMessage(new TextMessage(message));
                    log.debug("✅ Progress sent to session {}: {}%", httpSessionId, progress.getProgressPercentage());
                } catch (IOException e) {
                    log.error("Failed to send progress to session: {}", httpSessionId, e);
                    sessions.remove(webSocketSessionId);
                    httpToWebSocketSessionMapping.remove(httpSessionId);
                }
            }
        } else {
            log.warn("❌ No WebSocket session found for HTTP session: {} (connected sessions: {})", 
                httpSessionId, sessions.size());
            
            // 임시 대안: 브로드캐스트하되 requestId로 클라이언트에서 필터링하도록 함
            log.info("🔄 Falling back to broadcast for requestId: {}", progress.getRequestId());
            broadcastProgress(progress);
            
            // 디버깅을 위해 현재 매핑 상태 출력
            if (log.isDebugEnabled()) {
                debugSessionMappings();
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
     * URL 쿼리 파라미터에서 HTTP 세션 ID 추출
     */
    private String extractHttpSessionId(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            log.debug("Extracting sessionId from query: {}", query);
            
            if (query != null && query.contains("sessionId=")) {
                // sessionId=value&other=value 또는 sessionId=value 형태 처리
                String[] queryParts = query.split("&");
                for (String part : queryParts) {
                    if (part.startsWith("sessionId=")) {
                        String sessionId = part.substring("sessionId=".length());
                        String decoded = URLDecoder.decode(sessionId, StandardCharsets.UTF_8);
                        log.debug("Extracted sessionId: {} -> {}", sessionId, decoded);
                        return decoded;
                    }
                }
            }
            
            log.debug("No sessionId found in query: {}", query);
            return null;
        } catch (Exception e) {
            log.error("Error extracting HTTP session ID from WebSocket session", e);
            return null;
        }
    }
    
    /**
     * 활성 세션 수 조회
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
    
    /**
     * 디버깅용: 세션 매핑 상태 출력
     */
    public void debugSessionMappings() {
        log.info("=== WebSocket Session Mappings ===");
        log.info("Active WebSocket sessions: {}", sessions.size());
        log.info("HTTP->WebSocket mappings: {}", httpToWebSocketSessionMapping.size());
        httpToWebSocketSessionMapping.forEach((httpId, wsId) -> {
            WebSocketSession wsSession = sessions.get(wsId);
            boolean isOpen = wsSession != null && wsSession.isOpen();
            log.info("  {} -> {} (open: {})", httpId, wsId, isOpen);
        });
        log.info("================================");
    }
}
