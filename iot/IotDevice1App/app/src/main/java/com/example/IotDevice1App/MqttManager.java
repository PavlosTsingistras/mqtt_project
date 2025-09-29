package com.example.IotDevice1App;

import android.util.Log;
import org.eclipse.paho.client.mqttv3.*;

public class MqttManager {

    private static final String TAG = "MqttManager";
    private MqttClient mqttClient;
    private final String serverUri;
    private final String clientId;

    public MqttManager(String serverUri, String clientId) {  //diaxeirizetai conections, publishing
        this.serverUri = serverUri;
        this.clientId = clientId;
    }

    public void connect(IMqttActionListener connectionCallback) { //Establish conection to the broker
        try {
            mqttClient = new MqttClient(serverUri, clientId, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(2);

            mqttClient.connect(options);
            Log.d(TAG, "Connected to MQTT broker at " + serverUri);
            if (connectionCallback != null) {
                connectionCallback.onSuccess(null);
            }
        } catch (MqttException e) {
            Log.e(TAG, "Error during MQTT connection", e);
            if (connectionCallback != null) {
                connectionCallback.onFailure(null, e);
            }
            throw new RuntimeException(e);
        }
    }

    public void subscribe(String topic, int qos, IMqttActionListener subscriptionCallback) {
        try {
            mqttClient.subscribe(topic, qos);
            Log.d(TAG, "Subscribed to topic: " + topic);
            if (subscriptionCallback != null) {
                subscriptionCallback.onSuccess(null);
            }
        } catch (MqttException e) {
            Log.e(TAG, "Error during subscription", e);
            if (subscriptionCallback != null) {
                subscriptionCallback.onFailure(null, e);
            }
        }
    }

    //kanei publish se ena sygkekrimeno topic
    public void publish(String topic, String message, int qos, IMqttActionListener publishCallback) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(qos);
            mqttClient.publish(topic, mqttMessage);
            Log.d(TAG, "Message published to topic: " + topic);
            if (publishCallback != null) {
                publishCallback.onSuccess(null);
            }
        } catch (MqttException e) {
            Log.e(TAG, "Error during publish", e);
            if (publishCallback != null) {
                publishCallback.onFailure(null, e);
            }
        }
    }

    public void setCallback(MqttCallback callback) {
        if (mqttClient != null) {
            mqttClient.setCallback(callback);
        } else {
            Log.e(TAG, "Cannot set callback: MQTT client is not initialized.");
        }
    }

    public void disconnect() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                Log.d(TAG, "Disconnected from MQTT broker");
            }
        } catch (MqttException e) {
            Log.e(TAG, "Error during MQTT disconnection", e);
        }
    }
}
