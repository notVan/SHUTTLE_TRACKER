package com.shuttletracker.live;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** ws://localhost:8080/ws/fleet. The dashboard's map subscribes here (registered in WebSocketConfig). */
@Component
public class WebSocketLiveFeed extends TextWebSocketHandler implements LiveFeedPublisher {

    private static final Logger log = LoggerFactory.getLogger(WebSocketLiveFeed.class);

    private final JsonMapper jsonMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public WebSocketLiveFeed(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // Decorator makes sendMessage safe from several threads and drops slow clients.
        sessions.put(session.getId(), new ConcurrentWebSocketSessionDecorator(session, 5_000, 256 * 1024));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

    @Override
    public void broadcast(String type, Object data) {
        TextMessage message = new TextMessage(jsonMapper.writeValueAsString(Map.of("type", type, "data", data)));
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) session.sendMessage(message);
            } catch (IOException | IllegalStateException e) {
                log.debug("Dropping dashboard session {}: {}", session.getId(), e.getMessage());
                sessions.remove(session.getId());
            }
        });
    }
}
