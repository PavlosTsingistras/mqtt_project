package com.mqtt.edge_server.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.persistence.DiscriminatorValue;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("GAS")
@JsonTypeName("GAS")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GasSensor extends SensorReading {
    private static final double THRESHOLD = 9.15;

    public GasSensor(double value) {
        super("GAS", value);
    }

    @Override
    public boolean isOutOfRange() {
        return getValue() > THRESHOLD;
    }
}
