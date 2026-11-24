package com.shuttletracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** @param allowedOrigins frontend origins allowed to open ws://.../ws/fleet */
@ConfigurationProperties(prefix = "live-feed")
public record LiveFeedProperties(List<String> allowedOrigins) { }
