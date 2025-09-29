package com.mqtt.edge_server.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("SMOKE")
@JsonTypeName("SMOKE")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SmokeSensor extends SensorReading {
    private static final double THRESHOLD = 0.14;

    public SmokeSensor(double value) {
        super("SMOKE", value);
    }

    @Override
    public boolean isOutOfRange() {
        return getValue() > THRESHOLD;
    }
}
