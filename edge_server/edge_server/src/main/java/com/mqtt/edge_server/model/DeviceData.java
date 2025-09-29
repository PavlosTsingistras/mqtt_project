package com.mqtt.edge_server.model;
// class for creating device from the raw iot json payload
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import java.util.List;

@Data
@NoArgsConstructor
public class DeviceData {
    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("battery")
    private String battery;

    @JsonProperty("lat")
    private double lat;

    @JsonProperty("lon")
    private double lon;

    @JsonProperty("sensors")
    private List<SensorReading> sensors;

    public static DeviceData fromJson(String json) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, DeviceData.class);
        } catch (Exception e) {
            System.err.println("Error parsing JSON into DeviceData: " + e.getMessage());
            throw e;
        }
    }

    public SensorReading getSensor(String type) {
        if (sensors == null) return null;
        return sensors.stream().filter(s -> s.getType().equalsIgnoreCase(type)).findFirst().orElse(null);
    }
}
