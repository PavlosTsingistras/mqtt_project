package com.mqtt.edge_server.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("UV")
@JsonTypeName("UV")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UVSensor extends SensorReading {
    private static final double THRESHOLD = 6.0;

    public UVSensor(double value) {
        super("UV", value);
    }

    @Override
    public boolean isOutOfRange() {
        return getValue() > THRESHOLD;
    }
}
