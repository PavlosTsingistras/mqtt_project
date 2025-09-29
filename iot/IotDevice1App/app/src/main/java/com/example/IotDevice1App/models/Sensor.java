package com.example.IotDevice1App.models;

import java.io.Serializable;

public abstract class Sensor implements Serializable {
    private SensorType type;
    private double minValue;
    private double maxValue;
    private double currentValue;


    //To isEnabled diaxeirizete to on/off tou sensor gia apostolh data
    private boolean isEnabled = true;

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public Sensor(SensorType type, double minValue, double maxValue, double currentValue) {
        this.type = type;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.currentValue = currentValue;
    }

    public SensorType getType() {
        return type;
    }

    public double getMinValue() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        if (currentValue >= minValue && currentValue <= maxValue) {
            this.currentValue = currentValue;
        } else {
            throw new IllegalArgumentException("Value out of range");
        }
    }

    public abstract boolean exceedsThreshold();
}
