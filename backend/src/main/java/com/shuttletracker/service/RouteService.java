package com.shuttletracker.service;

import com.shuttletracker.dto.Dtos.RouteDto;
import com.shuttletracker.dto.Dtos.StopDto;
import com.shuttletracker.repository.RouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Routes and stops for the map. Editing routes is a Stage 3 item (for now: database/init SQL). */
@Service
public class RouteService {

    private final RouteRepository routeRepository;

    public RouteService(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @Transactional(readOnly = true)
    public List<RouteDto> list() {
        return routeRepository.findByActiveTrueOrderByName().stream()
                .map(r -> new RouteDto(r.getId(), r.getName(), r.getColor(), r.getStops().stream()
                        .map(s -> new StopDto(s.getId(), s.getName(), s.getSequence(), s.getLatitude(), s.getLongitude()))
                        .toList()))
                .toList();
    }
}
