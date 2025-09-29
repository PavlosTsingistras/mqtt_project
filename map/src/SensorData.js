
export class SensorData {

    constructor(id, label, lat, lng, battery, temperature, smoke, gas, uv, criticalStatus, isDis) {
        this.id = id;
        this.label = label;
        this.lat = lat;
        this.lng = lng;
        this.battery = battery;
        this.temperature = temperature;
        this.smoke = smoke;
        this.gas = gas;
        this.uv = uv;
        this.criticalStatus = criticalStatus;
        this.isDis = isDis;
    }
    
    isCritical() {
        return this.criticalStatus === "HIGH";
    }

    getStatus() {
        return this.criticalStatus === "HIGH" ? "Critical" : 
                this.criticalStatus === "MODERATE" ? "Moderate" : "Normal";
    }

    // logic gia ta state tou server
    static determineCriticalStatus(temperature, smoke, gas, uv) {
        
        // hard-coded limits, opws sto assignment paper
        const SMOKE_THRESHOLD = 0.14;
        const GAS_THRESHOLD = 9.15;
        const TEMP_THRESHOLD = 50;
        const UV_THRESHOLD = 6;

        // conv se flags
        const smokeHigh = smoke > SMOKE_THRESHOLD;
        const gasHigh = gas > GAS_THRESHOLD;
        const tempHigh = temperature > TEMP_THRESHOLD;
        const uvHigh = uv > UV_THRESHOLD;

        // the states are being chosen based on the assignment explanation
        // i.e 1) all sensors are high, 2) gas is high or 3) smoke & gas are high
        if ((smokeHigh && gasHigh) || (gasHigh) || (smokeHigh && gasHigh && tempHigh && uvHigh)) {

            return "HIGH"; // Critical
        } else if (!smokeHigh && !gasHigh && tempHigh && uvHigh) {  // if only uv and temperature are high

            return "MODERATE"; // Moderate
        }   // all else fails, remain normal
        
        return "NORMAL"; // Normal
    }

    static isDisabled(temperature, smoke, gas, uv) {
        return (temperature === -1 && smoke === -1 && gas === -1 && uv === -1);
    }

    // mapping from webscket to instace
    static fromWebSocket(obj) {

        // base (if no data was provided from error or anything else)
        let temperature = -1, smoke = -1, gas = -1, uv = -1;
        
        obj.sensors.forEach(sensor => {
            if (sensor.type === "TEMPERATURE") temperature = sensor.value;
            if (sensor.type === "SMOKE") smoke = sensor.value;
            if (sensor.type === "GAS") gas = sensor.value;
            if (sensor.type === "UV") uv = sensor.value;
        });

        const criticalStatus = SensorData.determineCriticalStatus(temperature, smoke, gas, uv);
        const isDis = this.isDisabled(temperature, smoke, gas, uv);

        return new SensorData(
            obj.device_id,
            `Device ${obj.device_id}`,
            obj.lat,
            obj.lon,
            obj.battery,
            temperature,
            smoke,
            gas,
            uv,
            criticalStatus,
            isDis
        );
    }
}
