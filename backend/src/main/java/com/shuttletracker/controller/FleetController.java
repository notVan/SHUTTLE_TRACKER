package com.shuttletracker.controller;

import com.shuttletracker.dto.Dtos.LivePositionDto;
import com.shuttletracker.service.FleetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Snapshot of where every shuttle is now. Live updates after that come over ws://.../ws/fleet. */
@RestController
@RequestMapping("/api/fleet")
public class FleetController {

    private final FleetService fleetService;

    public FleetController(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    @GetMapping("/live")
    public List<LivePositionDto> live() {
        return fleetService.livePositions();
    }
}
