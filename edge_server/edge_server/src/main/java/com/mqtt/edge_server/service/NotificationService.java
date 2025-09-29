package com.mqtt.edge_server.service;

import com.mqtt.edge_server.model.DeviceData;
import com.mqtt.edge_server.model.RiskLevel;
import org.springframework.stereotype.Service;
// old notification service class used mainly for logging
@Service
public class NotificationService {

    public void sendAlert(DeviceData deviceData, RiskLevel riskLevel) {
        String alertMessage = String.format(
                "Alert: %s detected near [%f, %f]",
                riskLevel, deviceData.getLat(), deviceData.getLon()
        );

        System.out.println("Sending alert: " + alertMessage);
    }
}
