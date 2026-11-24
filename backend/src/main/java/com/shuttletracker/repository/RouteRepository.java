package com.shuttletracker.repository;

import com.shuttletracker.model.Route;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Integer> {

    @EntityGraph(attributePaths = "stops")
    List<Route> findByActiveTrueOrderByName();
}
