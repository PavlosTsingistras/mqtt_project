import { SensorData } from './SensorData';

export const markers = [
    SensorData.fromObject({
        id: 2,
        label: "Sensor2",
        lat: 37.9674,
        lng: 23.7681,
        battery: 50,
        temperature: 18,
        humidity: 75,
        description: "Sunny",
        smoke: 0.25,
        gas: 9,
        uv: 9,
        criticalStatus: "HIGH"
    })
]; 