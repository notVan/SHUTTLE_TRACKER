package com.shuttletracker.traccar;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection settings for the Traccar server that decodes the ZX303 Topin stream.
 *
 * @param baseUrl          Traccar web/API address, e.g. http://localhost:8082
 * @param email            Traccar user the backend logs in as
 * @param password         that user's password
 * @param reconnectSeconds wait between attempts when the live socket drops
 */
@ConfigurationProperties(prefix = "traccar")
public record TraccarProperties(
        String baseUrl,
        String email,
        String password,
        int reconnectSeconds) { }
