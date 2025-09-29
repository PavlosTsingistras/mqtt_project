package com.mqtt.edge_server.model;
//risk event class with JPS for MySQL integration
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Entity
@Table(name = "risk_events")
@Data
@NoArgsConstructor
public class RiskEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_timestamp", nullable = false)
    private String eventTimestamp;

    @Column(name = "lat", nullable = false)
    private double lat;

    @Column(name = "lon", nullable = false)
    private double lon;

    @Column(name = "risk_level", nullable = false)
    private String riskLevel;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_event_id")
    private List<SensorReading> sensors;

    public RiskEvent(String eventTimestamp, double lat, double lon, String riskLevel, List<SensorReading> sensors) {
        this.eventTimestamp = eventTimestamp;
        this.lat = lat;
        this.lon = lon;
        this.riskLevel = riskLevel;
        this.sensors = sensors;
    }
}
