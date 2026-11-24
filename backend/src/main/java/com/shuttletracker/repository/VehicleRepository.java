package com.shuttletracker.repository;

import com.shuttletracker.model.Vehicle;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {

    @EntityGraph(attributePaths = "route")
    List<Vehicle> findByActiveTrueOrderByName();

    @EntityGraph(attributePaths = "route")
    Optional<Vehicle> findByTraccarDeviceId(Long traccarDeviceId);

    boolean existsByImei(String imei);
}
