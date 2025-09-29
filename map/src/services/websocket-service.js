
class WebSocketService {
  constructor() {

    // For mqtt-data stream:
    this.ws = null;
    this.callbacks = new Set();
    this.isConnected = false;

    // For android_location stream:
    this.androidWs = null;
    this.androidCallbacks = new Set();
    this.androidIsConnected = false;

  }

  // Connection for sensor (mqtt-data) stream
  connect() {
    if (this.ws) return;

    console.log('[INIT CONNECTION] Connecting to WebSocket endpoint at ws://localhost:8080/mqtt-data');
    this.ws = new WebSocket('ws://localhost:8080/mqtt-data');

    this.ws.onopen = () => {
      console.log('[CON-IOT] WebSocket connection established');
      this.isConnected = true;
    };

    this.ws.onmessage = (event) => {
      try {
        let data = event.data;

        // Attempt JSON parsing, if parsing fails, use the raw string.
        try {

          data = JSON.parse(event.data);
        } catch (err) {

          // Not JSON – use raw data.
        }
        console.log('[MES-IOT] Received message:', data);
        this.callbacks.forEach(callback => callback(data));
      } catch (error) {
        console.error('[ERROR] Error processing incoming message:', error);
      }
    };

    this.ws.onerror = (error) => {
      console.error('[ERROR] WebSocket error:', error);
      this.isConnected = false;
    };

    this.ws.onclose = (event) => {
      console.log('[ERROR] WebSocket connection closed:', event);
      this.isConnected = false;
      // Optionally try to reconnect after some time.
      setTimeout(() => this.connect(), 5000);
    };
  }

  // Subscribe to sensor (mqtt-data) messages.
  subscribe(callback) {
    if (typeof callback !== 'function') {
      throw new Error('Callback must be a function');
    }
    this.callbacks.add(callback);
    return () => this.callbacks.delete(callback);
  }

  // New methods for android_location stream
  connectAndroidLocation() {
    if (this.androidWs) return;

    // stream name 
    console.log('[INIT CONNECTION ANDR] Connecting to Android Location WebSocket endpoint at ws://localhost:8080/android-location');
    this.androidWs = new WebSocket('ws://localhost:8080/android-location');

    this.androidWs.onopen = () => {
      console.log('[CON-ANDR] Android Location WebSocket connection established');
      this.androidIsConnected = true;
    };

    this.androidWs.onmessage = (event) => {
      try {
        let data = event.data;
        
        // try Json parse, same as with the iot message format
        try {

          data = JSON.parse(event.data);
        } catch (err) {

          // Not JSON – use raw data.
        }

        console.log('[MES-ANDR] Received android location message:', data);
        this.androidCallbacks.forEach(callback => callback(data));
      } catch (error) {

        console.error('[ERROR] Error processing android location message:', error);
      }
    };

    // handle errors upon close
    this.androidWs.onerror = (error) => {

      console.error('[ERROR] Android Location WebSocket error:', error);
      this.androidIsConnected = false;
    };

    this.androidWs.onclose = (event) => {

      console.log('[ERROR] Android Location WebSocket connection closed:', event);
      this.androidIsConnected = false;
      setTimeout(() => this.connectAndroidLocation(), 5000);
    };
  }

  // Subscribe to android_location messages.
  subscribeAndroidLocation(callback) {

    if (typeof callback !== 'function') {

      throw new Error('Callback must be a function');
    }

    this.androidCallbacks.add(callback);
    return () => this.androidCallbacks.delete(callback);
  }

  // Disconnect both streams.
  disconnect() {
    if (this.ws) {
      console.log('[DISCON] Disconnecting mqtt-data WebSocket...');
      this.ws.close();
      this.ws = null;
      this.isConnected = false;
      this.callbacks.clear();
    }
    if (this.androidWs) {
      console.log('[DISCON] Disconnecting android location WebSocket...');
      this.androidWs.close();
      this.androidWs = null;
      this.androidIsConnected = false;
      this.androidCallbacks.clear();
    }
  }

  // Get connection status for the sensor stream.
  getConnectionStatus() {
    return this.isConnected;
  }

  // Get connection status for the android_location stream.
  getAndroidConnectionStatus() {
    return this.androidIsConnected;
  }
}

const websocketService = new WebSocketService();
export default websocketService;
