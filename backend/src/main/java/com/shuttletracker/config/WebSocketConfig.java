package com.shuttletracker.config;

import com.shuttletracker.live.WebSocketLiveFeed;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** Exposes the live feed at /ws/fleet for the dashboard's browser map. */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketLiveFeed liveFeed;
    private final LiveFeedProperties props;

    public WebSocketConfig(WebSocketLiveFeed liveFeed, LiveFeedProperties props) {
        this.liveFeed = liveFeed;
        this.props = props;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(liveFeed, "/ws/fleet")
                .setAllowedOrigins(props.allowedOrigins().toArray(String[]::new));
    }
}
