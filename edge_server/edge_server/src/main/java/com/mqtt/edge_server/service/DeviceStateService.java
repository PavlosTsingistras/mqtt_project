package com.mqtt.edge_server.service;

import com.mqtt.edge_server.model.DeviceData;
import com.mqtt.edge_server.model.LocationData;
import com.mqtt.edge_server.model.RiskLevel;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
// class for managing the states of the devices. We keep a previous state in
// memory using a hashmap to not have sync problems with the iot devices , android app and
// the payload sent to the map application using websockets
@Service
public class DeviceStateService {

    static class DeviceState {
        private LocationData location;
        private RiskLevel riskLevel;

        public DeviceState(LocationData location, RiskLevel riskLevel) {
            this.location = location;
            this.riskLevel = riskLevel;
        }

        public LocationData getLocation() {
            return location;
        }

        public RiskLevel getRiskLevel() {
            return riskLevel;
        }
    }

    private final Map<String, DeviceState> deviceStates = new HashMap<>();


    public void updateDeviceState(String deviceId, LocationData location, RiskLevel riskLevel) {
        deviceStates.put(deviceId, new DeviceState(location, riskLevel));
    }


    public DeviceState getDeviceState(String deviceId) {
        return deviceStates.get(deviceId);
    }


    public LocationData getConsolidatedAlertLocation() {
        LocationData device1Loc = deviceStates.get("1") != null && deviceStates.get("1").getRiskLevel() != RiskLevel.NONE
                ? deviceStates.get("1").getLocation()
                : null;

        LocationData device2Loc = deviceStates.get("2") != null && deviceStates.get("2").getRiskLevel() != RiskLevel.NONE
                ? deviceStates.get("2").getLocation()
                : null;

        if (device1Loc == null && device2Loc == null) {
            return null;
        }

        if (device1Loc != null && device2Loc == null) {
            return device1Loc;
        }
        if (device2Loc != null && device1Loc == null) {
            return device2Loc;
        }

        double avgLat = (device1Loc.getLat() + device2Loc.getLat()) / 2.0;
        double avgLon = (device1Loc.getLon() + device2Loc.getLon()) / 2.0;
        return new LocationData(avgLat, avgLon, 10);
    }


    public RiskLevel getOverallRisk() {
        RiskLevel highest = RiskLevel.NONE;
        for (DeviceState state : deviceStates.values()) {
            if (state.getRiskLevel().ordinal() > highest.ordinal()) {
                highest = state.getRiskLevel();
            }
        }
        return highest;
    }
}
