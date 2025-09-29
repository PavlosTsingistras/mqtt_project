export class AndroidDeviceData {
    constructor(id, label, lat, lng, battery, accuracy, timestamp, isOffline) {
      this.id = id;
      this.label = label;
      this.lat = lat;
      this.lng = lng;
      this.battery = battery;
    }

    // testing out some function
    // of course this is not going in production, i.e final build
    getStatus() {
      return this.battery < 20 ? "Low Battery" : "Normal";
    }
  
    // Create a webSocket instance
    static fromWebSocket(obj) {
      return new AndroidDeviceData(
        obj.device_id,
        `Android Device ${obj.device_id}`,
        obj.lat,
        obj.lon || obj.lng,                             // in case we have differences
        obj.battery !== undefined ? obj.battery : 100
      );
    }
  }
  