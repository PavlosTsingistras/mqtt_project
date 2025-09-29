package com.test.androidapp.mymqttapp.models;

public class LocationData {
    private double lat;
    private double lon;
    private int deviceId;

    public LocationData() { }

    public LocationData(double lat, double lon, int deviceId) {
        this.lat = lat;
        this.lon = lon;
        this.deviceId = deviceId;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public String toString() {
        return "LocationData [lat=" + lat + ", lon=" + lon + ", deviceId=" + deviceId + "]";
    }
}
