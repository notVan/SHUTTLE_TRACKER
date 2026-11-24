package com.shuttletracker.traccar;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Outbound calls to Traccar's REST API (http://localhost:8082/api/...).
 * The only class that knows Traccar's URLs; services call this, never Traccar directly.
 */
@Component
public class TraccarClient {

    private final TraccarProperties props;
    private final JsonMapper jsonMapper;
    private final RestClient http;

    private volatile String sessionCookie;

    public TraccarClient(TraccarProperties props, JsonMapper jsonMapper) {
        this.props = props;
        this.jsonMapper = jsonMapper;
        this.http = RestClient.builder().baseUrl(props.baseUrl()).build();
    }

    /** Logs in once (POST /api/session) and reuses the JSESSIONID cookie until it stops working. */
    public synchronized String sessionCookie() {
        if (sessionCookie == null) {
            String form = "email=" + encode(props.email()) + "&password=" + encode(props.password());
            ResponseEntity<String> response = http.post()
                    .uri("/api/session")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toEntity(String.class);
            sessionCookie = response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE).stream()
                    .map(cookie -> cookie.split(";", 2)[0])
                    .filter(cookie -> cookie.startsWith("JSESSIONID="))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Traccar login returned no session cookie"));
        }
        return sessionCookie;
    }

    public synchronized void invalidateSession() {
        sessionCookie = null;
    }

    /** Traccar's device id for a tracker IMEI (Traccar calls the IMEI "uniqueId"). */
    public Optional<Long> findDeviceId(String imei) {
        JsonNode devices = jsonMapper.readTree(withSession(cookie -> http.get()
                .uri("/api/devices?uniqueId={imei}", imei)
                .header(HttpHeaders.COOKIE, cookie)
                .retrieve()
                .body(String.class)));
        return devices.isArray() && !devices.isEmpty()
                ? Optional.of(devices.get(0).get("id").asLong())
                : Optional.empty();
    }

    /** Registers a tracker in Traccar so it accepts that IMEI's frames. Returns Traccar's device id. */
    public long createDevice(String name, String imei) {
        String body = jsonMapper.writeValueAsString(Map.of("name", name, "uniqueId", imei));
        JsonNode created = jsonMapper.readTree(withSession(cookie -> http.post()
                .uri("/api/devices")
                .header(HttpHeaders.COOKIE, cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class)));
        return created.get("id").asLong();
    }

    public long ensureDevice(String name, String imei) {
        return findDeviceId(imei).orElseGet(() -> createDevice(name, imei));
    }

    /** ws://host:8082/api/socket, Traccar's live stream of positions, devices and events. */
    public URI socketUri() {
        return URI.create(props.baseUrl().replaceFirst("^http", "ws") + "/api/socket");
    }

    private String withSession(Function<String, String> call) {
        try {
            return call.apply(sessionCookie());
        } catch (HttpClientErrorException.Unauthorized e) {
            invalidateSession();            // session expired: log in again once
            return call.apply(sessionCookie());
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
