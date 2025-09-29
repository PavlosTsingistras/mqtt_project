package com.mqtt.edge_server.model;
//sensor abstract class with concrete implemnetation in the other files
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "sensor_type", discriminatorType = DiscriminatorType.STRING)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SmokeSensor.class, name = "SMOKE"),
        @JsonSubTypes.Type(value = GasSensor.class, name = "GAS"),
        @JsonSubTypes.Type(value = TemperatureSensor.class, name = "TEMPERATURE"),
        @JsonSubTypes.Type(value = UVSensor.class, name = "UV")
})
public abstract class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    protected String type;
    private double value;

    public SensorReading(String type, double value) {
        this.type = type;
        this.value = value;
    }



    public abstract boolean isOutOfRange();
}
