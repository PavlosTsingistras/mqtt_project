package com.test.androidapp.mymqttapp.models;

public class Alert {
    private String riskLevel;
    private int distance;

    public Alert() { }

    public Alert(String riskLevel, int distance) {
        this.riskLevel = riskLevel;
        this.distance = distance;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    @Override
    public String toString() {
        return "Alert [riskLevel=" + riskLevel + ", distance=" + distance + "]";
    }
}
