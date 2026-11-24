package com.shuttletracker.traccar;

import com.shuttletracker.service.FleetService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Entry point for live tracker data. Holds a WebSocket open to Traccar's /api/socket,
 * parses each message and hands positions to the service layer. It never touches
 * repositories directly. Reconnects on its own if Traccar restarts.
 */
@Component
public class TraccarSocketListener implements WebSocket.Listener {

    private static final Logger log = LoggerFactory.getLogger(TraccarSocketListener.class);

    private final TraccarClient traccar;
    private final TraccarProperties props;
    private final JsonMapper jsonMapper;
    private final FleetService fleetService;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "traccar-socket");
        t.setDaemon(true);
        return t;
    });
    private final StringBuilder partial = new StringBuilder();
    private volatile boolean running;

    public TraccarSocketListener(TraccarClient traccar, TraccarProperties props,
                                 JsonMapper jsonMapper, FleetService fleetService) {
        this.traccar = traccar;
        this.props = props;
        this.jsonMapper = jsonMapper;
        this.fleetService = fleetService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        running = true;
        scheduler.execute(this::connect);
    }

    @PreDestroy
    public void stop() {
        running = false;
        scheduler.shutdownNow();
    }

    private void connect() {
        if (!running) return;
        try {
            httpClient.newWebSocketBuilder()
                    .header("Cookie", traccar.sessionCookie())
                    .buildAsync(traccar.socketUri(), this)
                    .whenComplete((ws, err) -> {
                        if (err != null) retry("connect failed: " + err.getMessage());
                        else log.info("Listening to Traccar live socket {}", traccar.socketUri());
                    });
        } catch (RuntimeException e) {
            retry("login failed: " + e.getMessage());
        }
    }

    private void retry(String reason) {
        if (!running) return;
        traccar.invalidateSession();
        log.warn("Traccar socket {} - retrying in {}s", reason, props.reconnectSeconds());
        scheduler.schedule(this::connect, props.reconnectSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        partial.append(data);
        if (last) {
            String message = partial.toString();
            partial.setLength(0);
            handle(message);
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        retry("closed (" + statusCode + " " + reason + ")");
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        retry("error: " + error.getMessage());
    }

    /** Traccar sends {"positions":[...]}, {"devices":[...]} or {"events":[...]} messages. */
    private void handle(String message) {
        try {
            JsonNode node = jsonMapper.readTree(message);
            if (node.has("positions")) {
                fleetService.onTraccarPositions(node.get("positions"));
            }
            // TODO Stage 3: node "devices" -> online/offline status, node "events" -> alerts.
        } catch (RuntimeException e) {
            log.warn("Ignoring Traccar message ({}): {}", e.getMessage(), message);
        }
    }
}
