package com.example.IotDevice2App.models;

public class UVSensor extends Sensor {

    private static final double THRESHOLD = 6.0;

    public UVSensor(double minValue, double maxValue, double currentValue) {
        super(SensorType.UV, minValue, maxValue, currentValue);
    }

    @Override
    public boolean exceedsThreshold() {
        return getCurrentValue() > THRESHOLD;
    }
}
