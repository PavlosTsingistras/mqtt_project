package com.example.IotDevice2App.models;

public class GasSensor extends Sensor {

    private static final double THRESHOLD = 9.15;

    public GasSensor(double minValue, double maxValue, double currentValue) {
        super(SensorType.GAS, minValue, maxValue, currentValue);
    }

    @Override
    public boolean exceedsThreshold() {
        return getCurrentValue() > THRESHOLD;
    }
}
