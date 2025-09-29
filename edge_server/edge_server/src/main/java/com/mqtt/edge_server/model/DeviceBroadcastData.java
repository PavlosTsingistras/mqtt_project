package com.mqtt.edge_server.model;
//class for broadcasting iot device data to websockets
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class DeviceBroadcastData {

    @JsonProperty("device_id")
    private String deviceId;

    @JsonProperty("battery")
    private int battery;

    @JsonProperty("lat")
    private double lat;

    @JsonProperty("lon")
    private double lon;

    @JsonProperty("criticalStatus")
    private String criticalStatus;

    @JsonProperty("sensors")
    private List<SensorReading> sensors;

    public DeviceBroadcastData(String deviceId, int battery, double lat, double lon, String criticalStatus, List<SensorReading> sensors) {
        this.deviceId = deviceId;
        this.battery = battery;
        this.lat = lat;
        this.lon = lon;
        this.criticalStatus = criticalStatus;
        this.sensors = sensors;
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }
}
