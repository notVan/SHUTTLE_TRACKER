package com.shuttletracker.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/** A shuttle route, drawn on the map with its stops. */
@Entity
@Table(name = "routes")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /** Hex colour used on the map, e.g. #e5484d. */
    @Column(nullable = false, length = 7)
    private String color;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "route", fetch = FetchType.LAZY)
    @OrderBy("sequence ASC")
    private List<Stop> stops = new ArrayList<>();

    public Integer getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public List<Stop> getStops() { return stops; }
}
