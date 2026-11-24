package com.shuttletracker.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** A shuttle and the ZX303 tracker fitted to it (bound by the tracker's 15-digit IMEI). */
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(unique = true, length = 20)
    private String plate;

    @Column(nullable = false, unique = true, length = 15)
    private String imei;

    /** Traccar's id for this tracker; set once the backend has registered the IMEI in Traccar. */
    @Column(name = "traccar_device_id", unique = true)
    private Long traccarDeviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Integer getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }
    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }
    public Long getTraccarDeviceId() { return traccarDeviceId; }
    public void setTraccarDeviceId(Long traccarDeviceId) { this.traccarDeviceId = traccarDeviceId; }
    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
