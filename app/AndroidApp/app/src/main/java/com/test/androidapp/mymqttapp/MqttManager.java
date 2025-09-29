package com.test.androidapp.mymqttapp;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.*;

public class MqttManager {

    private static final String TAG = "MqttManager";
    private MqttClient mqttClient;
    private final String serverUri;
    private final String clientId;

    public MqttManager(String serverUri, String clientId) {
        this.serverUri = serverUri;
        this.clientId = clientId;
    }

    public void connect(IMqttActionListener callback) {
        try {
            mqttClient = new MqttClient(serverUri, clientId, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(2);
            mqttClient.connect(options);
            Log.d(TAG, "Connected to MQTT broker at " + serverUri);

            if (callback != null) {
                callback.onSuccess(null);
            }
        } catch (MqttException e) {
            Log.e(TAG, "Error during MQTT connection", e);
            if (callback != null) {
                callback.onFailure(null, e);
            }
        }
    }

    public void subscribe(String topic, int qos, IMqttMessageListener listener) {
        try {
            mqttClient.subscribe(topic, qos, listener);
            Log.d(TAG, "Subscribed to topic with IMqttMessageListener: " + topic);
        } catch (MqttException e) {
            Log.e(TAG, "Error subscribing with IMqttMessageListener", e);
        }
    }

    public void publish(String topic, String message, int qos, IMqttActionListener callback) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(qos);
            mqttClient.publish(topic, mqttMessage);
            Log.d(TAG, "Published: " + message);
            if (callback != null) {
                callback.onSuccess(null);
            }
        } catch (MqttException e) {
            Log.e(TAG, "Error during publish", e);
            if (callback != null) {
                callback.onFailure(null, e);
            }
        }
    }

    public void publish(String topic, String message) {
        publish(topic, message, 1, null);
    }


    public void disconnect() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                Log.d(TAG, "Disconnected from MQTT broker");
            }
        } catch (MqttException e) {
            Log.e(TAG, "Error during disconnection", e);
        }
    }
}