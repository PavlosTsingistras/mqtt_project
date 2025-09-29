package com.example.IotDevice2App.models;

public class TemperatureSensor extends Sensor {

    private static final double THRESHOLD = 50.0;

    public TemperatureSensor(double minValue, double maxValue, double currentValue) {
        super(SensorType.TEMPERATURE, minValue, maxValue, currentValue);
    }

    @Override
    public boolean exceedsThreshold() {
        return getCurrentValue() > THRESHOLD;
    }
}
