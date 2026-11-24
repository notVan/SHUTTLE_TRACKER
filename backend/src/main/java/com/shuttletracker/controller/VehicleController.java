package com.shuttletracker.controller;

import com.shuttletracker.dto.Dtos.VehicleDto;
import com.shuttletracker.dto.Dtos.VehicleRequest;
import com.shuttletracker.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public List<VehicleDto> list() {
        return vehicleService.list();
    }

    /** Registers a shuttle and its tracker IMEI (also creates the device in Traccar). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleDto create(@Valid @RequestBody VehicleRequest request) {
        return vehicleService.create(request);
    }
}
