package com.mqtt.edge_server.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("TEMPERATURE")
@JsonTypeName("TEMPERATURE")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TemperatureSensor extends SensorReading {
    private static final double THRESHOLD = 50.0; 

    public TemperatureSensor(double value) {
        super("TEMPERATURE", value);
    }

    @Override
    public boolean isOutOfRange() {
        return getValue() > THRESHOLD;
    }
}
