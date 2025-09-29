package com.mqtt.edge_server.service;

import com.mqtt.edge_server.model.DeviceData;
import com.mqtt.edge_server.model.RiskLevel;
import com.mqtt.edge_server.model.SensorReading;
import org.springframework.stereotype.Service;
// this class is responsible for determining the risk of the current alerts
@Service
public class RiskEvaluatorService {

    public RiskLevel evaluateRisk(DeviceData deviceData) {
        boolean smokeExceeded = false;
        boolean gasExceeded = false;
        boolean temperatureExceeded = false;
        boolean uvExceeded = false;

        for (SensorReading sensor : deviceData.getSensors()) {
            String sensorType = sensor.getType().toLowerCase();
            boolean exceeded = sensor.isOutOfRange();

            System.out.println(sensorType + " sensor exceeded: " + exceeded + " (value=" + sensor.getValue() + ")");

            switch (sensorType) {
                case "smoke":
                    smokeExceeded = exceeded;
                    break;
                case "gas":
                    gasExceeded = exceeded;
                    break;
                case "temperature":
                    temperatureExceeded = exceeded;
                    break;
                case "uv":
                    uvExceeded = exceeded;
                    break;
                default:
                    break;
            }
        }
        if (gasExceeded) {
            return RiskLevel.HIGH;
        }
        if (smokeExceeded && uvExceeded) {
            return RiskLevel.HIGH;
        }
        if (temperatureExceeded && uvExceeded) {
            return RiskLevel.MODERATE;
        }
        return RiskLevel.NONE;
    }
}
