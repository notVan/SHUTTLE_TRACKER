package com.shuttletracker.service;

import com.shuttletracker.dto.Dtos.LivePositionDto;
import com.shuttletracker.live.LiveFeedPublisher;
import com.shuttletracker.model.Vehicle;
import com.shuttletracker.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Turns Traccar positions into shuttle positions: Traccar device id -> vehicle (IMEI binding),
 * keeps the latest fix per vehicle in memory and pushes it to the dashboards.
 */
@Service
public class FleetService {

    private static final Logger log = LoggerFactory.getLogger(FleetService.class);
    private static final double KNOTS_TO_KMH = 1.852;   // Traccar reports speed in knots

    private final VehicleRepository vehicleRepository;
    private final LiveFeedPublisher liveFeed;
    private final Map<Integer, LivePositionDto> latest = new ConcurrentHashMap<>();

    public FleetService(VehicleRepository vehicleRepository, LiveFeedPublisher liveFeed) {
        this.vehicleRepository = vehicleRepository;
        this.liveFeed = liveFeed;
    }

    public List<LivePositionDto> livePositions() {
        return List.copyOf(latest.values());
    }

    /** Called by TraccarSocketListener with the "positions" array of one Traccar socket message. */
    public void onTraccarPositions(JsonNode positions) {
        for (JsonNode p : positions) {
            long deviceId = p.path("deviceId").asLong();
            // TODO: cache device id -> vehicle instead of one query per fix once the fleet grows.
            Vehicle vehicle = vehicleRepository.findByTraccarDeviceId(deviceId).orElse(null);
            if (vehicle == null) {
                log.debug("Position from Traccar device {} which is not bound to a vehicle", deviceId);
                continue;
            }
            LivePositionDto dto = new LivePositionDto(
                    vehicle.getId(),
                    vehicle.getName(),
                    vehicle.getRoute() == null ? null : vehicle.getRoute().getId(),
                    p.path("latitude").asDouble(),
                    p.path("longitude").asDouble(),
                    p.path("speed").asDouble() * KNOTS_TO_KMH,
                    p.path("course").asDouble(),
                    p.path("attributes").path("batteryLevel").isNumber()
                            ? p.path("attributes").path("batteryLevel").asInt() : null,
                    parseTime(p.path("fixTime")));
            latest.put(vehicle.getId(), dto);
            liveFeed.broadcast("position", dto);
        }
        // TODO Stage 3: next stop + ETA per shuttle, geofence alerts, trip history.
    }

    private static Instant parseTime(JsonNode node) {
        return node.isString() ? OffsetDateTime.parse(node.asString()).toInstant() : Instant.now();
    }
}
