package com.shuttletracker.service;

import com.shuttletracker.dto.Dtos.VehicleDto;
import com.shuttletracker.dto.Dtos.VehicleRequest;
import com.shuttletracker.model.Route;
import com.shuttletracker.model.Vehicle;
import com.shuttletracker.repository.RouteRepository;
import com.shuttletracker.repository.VehicleRepository;
import com.shuttletracker.traccar.TraccarClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Registers shuttles and binds each one's tracker IMEI to a Traccar device. */
@Service
public class VehicleService {

    private static final Logger log = LoggerFactory.getLogger(VehicleService.class);

    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;
    private final TraccarClient traccar;

    public VehicleService(VehicleRepository vehicleRepository, RouteRepository routeRepository, TraccarClient traccar) {
        this.vehicleRepository = vehicleRepository;
        this.routeRepository = routeRepository;
        this.traccar = traccar;
    }

    @Transactional(readOnly = true)
    public List<VehicleDto> list() {
        return vehicleRepository.findByActiveTrueOrderByName().stream().map(VehicleService::toDto).toList();
    }

    @Transactional
    public VehicleDto create(VehicleRequest request) {
        if (vehicleRepository.existsByImei(request.imei())) {
            throw new ConflictException("IMEI " + request.imei() + " is already registered");
        }
        Route route = request.routeId() == null ? null : routeRepository.findById(request.routeId())
                .orElseThrow(() -> new NotFoundException("Route " + request.routeId() + " not found"));

        Vehicle vehicle = new Vehicle();
        vehicle.setName(request.name());
        vehicle.setPlate(request.plate());
        vehicle.setImei(request.imei());
        vehicle.setRoute(route);
        linkToTraccar(vehicle);
        return toDto(vehicleRepository.save(vehicle));
    }

    /** Vehicles added straight into the database (e.g. sample data) get linked on startup. */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void syncWithTraccar() {
        vehicleRepository.findByActiveTrueOrderByName().stream()
                .filter(v -> v.getTraccarDeviceId() == null)
                .forEach(this::linkToTraccar);
    }

    private void linkToTraccar(Vehicle vehicle) {
        try {
            vehicle.setTraccarDeviceId(traccar.ensureDevice(vehicle.getName(), vehicle.getImei()));
        } catch (RuntimeException e) {
            // Traccar down or not set up yet: keep the vehicle, link it on the next start.
            log.warn("Could not register IMEI {} in Traccar yet: {}", vehicle.getImei(), e.getMessage());
        }
    }

    private static VehicleDto toDto(Vehicle v) {
        return new VehicleDto(v.getId(), v.getName(), v.getPlate(), v.getImei(),
                v.getRoute() == null ? null : v.getRoute().getId(),
                v.getRoute() == null ? null : v.getRoute().getName(),
                v.getTraccarDeviceId() != null);
    }
}
