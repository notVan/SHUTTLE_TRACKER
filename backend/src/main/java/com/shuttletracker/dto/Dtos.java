package com.shuttletracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/** Request/response records for the REST API and the live WebSocket feed. */
public final class Dtos {

    private Dtos() { }

    public record StopDto(Integer id, String name, Integer sequence, Double latitude, Double longitude) { }

    public record RouteDto(Integer id, String name, String color, List<StopDto> stops) { }

    public record VehicleDto(Integer id, String name, String plate, String imei,
                             Integer routeId, String routeName, boolean linkedToTraccar) { }

    public record VehicleRequest(
            @NotBlank @Size(max = 50) String name,
            @Size(max = 20) String plate,
            @NotBlank @Pattern(regexp = "\\d{15}", message = "IMEI must be 15 digits") String imei,
            Integer routeId) { }

    /**
     * Latest known position of one shuttle. Sent by GET /api/fleet/live and pushed
     * on ws://.../ws/fleet whenever Traccar reports a new fix.
     */
    public record LivePositionDto(Integer vehicleId, String vehicleName, Integer routeId,
                                  double latitude, double longitude,
                                  double speedKmh, double course,
                                  Integer batteryLevel, Instant fixTime) { }
}
