package com.example.IotDevice1App.models;

public class SmokeSensor extends Sensor {

    private static final double THRESHOLD = 0.14;

    public SmokeSensor(double minValue, double maxValue, double currentValue) {
        super(SensorType.SMOKE, minValue, maxValue, currentValue);
    }

    @Override
    public boolean exceedsThreshold() {
        return getCurrentValue() > THRESHOLD;
    }
}
