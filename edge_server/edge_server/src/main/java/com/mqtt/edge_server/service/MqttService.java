package com.mqtt.edge_server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mqtt.edge_server.model.*;
import com.mqtt.edge_server.repository.RiskEventRepository;
import com.mqtt.edge_server.websocket.WebSocketHandler;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
//class for mqtt callbacks , connection etc
@Service
@Slf4j
public class MqttService {

    private final MqttClient mqttClient;
    private final RiskEvaluatorService riskEvaluatorService;
    private final WebSocketHandler mqttWebSocketHandler;
    private final WebSocketHandler androidLocationWebSocketHandler;
    private final RiskEventRepository riskEventRepository;
    private final DeviceStateService deviceStateService;

    @Value("${mqtt.subscribe.topic1}")
    private String deviceTopic1;

    @Value("${mqtt.subscribe.topic2}")
    private String deviceTopic2;

    @Value("${mqtt.subscribe.android.topic}")
    private String androidLocationTopic;

    @Value("${mqtt.publish.topic}")
    private String androidNotificationsTopic;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private LocationData lastKnownPhoneLocation;

    @Autowired
    public MqttService(MqttClient mqttClient,
                       RiskEvaluatorService riskEvaluatorService,
                       @Qualifier("mqttDataHandler") WebSocketHandler mqttWebSocketHandler,
                       @Qualifier("androidLocationHandler") WebSocketHandler androidLocationWebSocketHandler,
                       RiskEventRepository riskEventRepository,
                       DeviceStateService deviceStateService) {
        this.mqttClient = mqttClient;
        this.riskEvaluatorService = riskEvaluatorService;
        this.mqttWebSocketHandler = mqttWebSocketHandler;
        this.androidLocationWebSocketHandler = androidLocationWebSocketHandler;
        this.riskEventRepository = riskEventRepository;
        this.deviceStateService = deviceStateService;
    }

    @PostConstruct
    public void initializeListener() {
        try {
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.error("MQTT connection lost: {}", cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    //every time a message arrives from topic
                    String payload = new String(message.getPayload());
                    log.info("MQTT Message received on [{}]: {}", topic, payload);

                    if (topic.equalsIgnoreCase(androidLocationTopic)) {
                        //if it is from the androidLocationTopic
                        processAndroidLocation(payload);
                    }
                    else if (topic.equalsIgnoreCase(deviceTopic1)) {
                        //if it is from iot device 1
                        processIotDeviceData("1", payload);
                    }
                    else if (topic.equalsIgnoreCase(deviceTopic2)) {
                        //if it is from iot device 2
                        processIotDeviceData("2", payload);
                    }
                    else {
                        log.warn("Received message on unknown topic: {}", topic);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    log.info("MQTT Message delivery complete.");
                }
            });

            mqttClient.subscribe(deviceTopic1);
            mqttClient.subscribe(deviceTopic2);
            log.info("Subscribed to device topics: {}, {}", deviceTopic1, deviceTopic2);

            mqttClient.subscribe(androidLocationTopic);
            log.info("Subscribed to Android location topic: {}", androidLocationTopic);

        } catch (MqttException e) {
            throw new RuntimeException("Failed to initialize MQTT listener: " + e.getMessage(), e);
        }
    }

    private void processAndroidLocation(String payload) {
        try {
            // we have a variable for the lastKnownLocation of the user
            LocationData locationData = LocationData.fromJson(payload);
            if (locationData != null) {
                //we update it every time we get a new location
                lastKnownPhoneLocation = locationData;
                log.info("Android location updated: {}", locationData.toJson());
                //sending the location to the web socket for the map to have
                androidLocationWebSocketHandler.broadcast(locationData.toJson());
            } else {
                log.warn("android/location message parsed to null.");
            }
        } catch (Exception e) {
            log.error("Error processing android/location message: {}", e.getMessage(), e);
        }
    }

    private void processIotDeviceData(String deviceId, String payload) {
        try {
            DeviceData deviceData = DeviceData.fromJson(payload);
            log.info("Device #{} Data Parsed: {}", deviceId, deviceData);

            RiskLevel riskLevel = riskEvaluatorService.evaluateRisk(deviceData);

            double lat = deviceData.getLat();
            double lon = deviceData.getLon();
            LocationData iotLocation = new LocationData(lat, lon, 10);
            //updating the new state with the new device
            deviceStateService.updateDeviceState(deviceId, iotLocation, riskLevel);

            DeviceBroadcastData broadcastData = new DeviceBroadcastData(
                    deviceData.getDeviceId(),
                    Integer.parseInt(deviceData.getBattery()),
                    lat,
                    lon,
                    riskLevel.toString(),
                    deviceData.getSensors()
            );
            // broadcastin the device data to the websocket
            String broadcastPayload = broadcastData.toJson();
            log.info("IoT device #{} data sent to /mqtt-data: {}", deviceId, broadcastPayload);
            mqttWebSocketHandler.broadcast(broadcastPayload);

            if (riskLevel != RiskLevel.NONE) {
                // if there is risk save it to the db
                saveRiskEvent(deviceData, riskLevel);

                if (lastKnownPhoneLocation == null) {
                    log.warn("Risk detected for device #{} but no phone location known yet.", deviceId);
                    return;
                }
                //send one alert always ,not 2 if both sensors are out of range
                LocationData consolidated = deviceStateService.getConsolidatedAlertLocation();
                if (consolidated != null) {
                    double distance = calculateDistance(
                            consolidated.getLat(),
                            consolidated.getLon(),
                            lastKnownPhoneLocation.getLat(),
                            lastKnownPhoneLocation.getLon()
                    );
                    //send the alert to the android device
                    sendAlert(riskLevel, (int) distance);
                }
            } else {
                log.info("No risk detected for device #{}.", deviceId);
            }

        } catch (Exception e) {
            log.error("Error processing device data message: {}", e.getMessage(), e);
        }
    }


    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371e3;
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private void sendAlert(RiskLevel riskLevel, int distance) {
        try {
            Alert alert = new Alert(riskLevel.toString(), distance);
            String alertJson = alert.toJson();
            log.info("Alert sent to {}: {}", androidNotificationsTopic, alertJson);
            MqttMessage alertMessage = new MqttMessage(alertJson.getBytes());
            alertMessage.setQos(1); //important
            //publish the alert for the android device to intercept
            mqttClient.publish(androidNotificationsTopic, alertMessage);
        } catch (Exception e) {
            log.error("Error sending alert to MQTT: {}", e.getMessage(), e);
        }
    }

    private void saveRiskEvent(DeviceData deviceData, RiskLevel riskLevel) {
        String formattedTimestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
        RiskEvent riskEvent = new RiskEvent(
                formattedTimestamp,
                deviceData.getLat(),
                deviceData.getLon(),
                riskLevel.toString(),
                deviceData.getSensors()
        );
        riskEventRepository.save(riskEvent);
    }

    public void publishMessage(String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1);
            mqttClient.publish(androidNotificationsTopic, mqttMessage);
            log.info("Message published to MQTT topic [{}]: {}", androidNotificationsTopic, message);
        } catch (MqttException e) {
            throw new RuntimeException("Failed to publish MQTT message: " + e.getMessage(), e);
        }
    }
}
