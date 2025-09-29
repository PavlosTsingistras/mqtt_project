package com.example.IotDevice2App;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.IotDevice2App.Services.BatteryService;
import com.example.IotDevice2App.Services.Location;
import com.example.IotDevice2App.models.GasSensor;
import com.example.IotDevice2App.models.Sensor;
import com.example.IotDevice2App.models.SmokeSensor;
import com.example.IotDevice2App.models.TemperatureSensor;
import com.example.IotDevice2App.models.UVSensor;
import com.example.IotDevice2App.Services.LocationService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String MQTT_CLIENT_ID = "AndroidClient2";
    private static final String PUBLISH_TOPIC = "iot/2";

    private MqttManager mqttManager;
    private ScheduledExecutorService scheduler;
    private SensorSliderFragment sensorFragment;
    private LocationService locationService;
    private BatteryService batteryService;
    private boolean isManualLocationMode = false;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private void checkAndRequestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkAndRequestLocationPermissions();

        //Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Sensor List");
        }

        // Initialize
        locationService = LocationService.getInstance(this);
        batteryService = BatteryService.getInstance(this);

        addSensorFragment();

        // Ask user for server ip and port:
        showMqttConfigDialog();
    }
    private void addSensorFragment() {
        ArrayList<Sensor> sensors = new ArrayList<>();
        sensors.add(new SmokeSensor(0.0, 0.25, 0.0));
        sensors.add(new GasSensor(0.0, 11.0, 0.0));

        sensorFragment = SensorSliderFragment.newInstance(sensors);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, sensorFragment)
                .commit();
    }
    private void connectToMqttBroker() {
        mqttManager.connect(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d(TAG, "Connected to MQTT broker");
                startPublishing();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.e(TAG, "Failed to connect to MQTT broker", exception);
            }
        });
    }
    private void startPublishing() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                List<Sensor> allSensors = sensorFragment.getCurrentSensorValues();
                List<Sensor> activeSensors = new ArrayList<>();
                for (Sensor sensor : allSensors) {
                    if (sensor.isEnabled()) {
                        activeSensors.add(sensor);
                    }
                }
                String jsonPayload = createJsonPayload(activeSensors);
                mqttManager.publish(PUBLISH_TOPIC, jsonPayload, 1, null);
                Log.d(TAG, "Published JSON: " + jsonPayload);
            } catch (Exception e) {
                Log.e(TAG, "Error while publishing data", e);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
    private String createJsonPayload(List<Sensor> sensors) {
        JsonObject json = new JsonObject();
        json.addProperty("deviceId", 2);
        json.addProperty("battery", batteryService.getBatteryPercentage());

        Location location = locationService.getCurrentLocation();
        json.addProperty("lat", location.getLatitude());
        json.addProperty("lon", location.getLongitude());

        JsonArray sensorArray = new JsonArray();
        for (Sensor sensor : sensors) {
            JsonObject sensorJson = new JsonObject();
            sensorJson.addProperty("type", sensor.getType().toString());
            sensorJson.addProperty("value", sensor.getCurrentValue());
            sensorArray.add(sensorJson);
        }
        json.add("sensors", sensorArray);

        return json.toString();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        mqttManager.disconnect();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_add_sensor) {
            showAddSensorDialog();
            return true;
        } else if (item.getItemId() == R.id.action_location_mode) {
            toggleLocationMode();
            return true;
        } else if (item.getItemId() == R.id.action_configure_mqtt) {
            showMqttConfigDialog();
            return true;
        } else if (item.getItemId() == R.id.action_exit) {
            showExitConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAddSensorDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_sensor, null);
        Spinner sensorTypeSpinner = dialogView.findViewById(R.id.sensorTypeSpinner);
        EditText minValueInput = dialogView.findViewById(R.id.minValueInput);
        EditText maxValueInput = dialogView.findViewById(R.id.maxValueInput);
        EditText currentValueInput = dialogView.findViewById(R.id.currentValueInput);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"UV", "Temperature"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sensorTypeSpinner.setAdapter(adapter);

        new AlertDialog.Builder(this)
                .setTitle("Add New Sensor")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String selectedType = (String) sensorTypeSpinner.getSelectedItem();
                    double minValue = Double.parseDouble(minValueInput.getText().toString());
                    double maxValue = Double.parseDouble(maxValueInput.getText().toString());
                    double currentValue = Double.parseDouble(currentValueInput.getText().toString());

                    Sensor newSensor;
                    if ("UV".equals(selectedType)) {
                        newSensor = new UVSensor(minValue, maxValue, currentValue);
                    } else {
                        newSensor = new TemperatureSensor(minValue, maxValue, currentValue);
                    }
                    addSensorToFragment(newSensor);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void addSensorToFragment(Sensor newSensor) {
        if (sensorFragment != null) {
            sensorFragment.addSensor(newSensor);
        }
    }


    private void showMqttConfigDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_mqtt_config, null);
        EditText ipInput = dialogView.findViewById(R.id.mqtt_ip_input);
        EditText portInput = dialogView.findViewById(R.id.mqtt_port_input);

        new AlertDialog.Builder(this)
                .setTitle("Enter MQTT Server Details")
                .setView(dialogView)
                .setCancelable(false)  // Prevents the user from skipping
                .setPositiveButton("Connect", (dialog, which) -> {
                    String ip = ipInput.getText().toString().trim();
                    String port = portInput.getText().toString().trim();

                    if (!ip.isEmpty() && !port.isEmpty()) {
                        String mqttUri = "tcp://" + ip + ":" + port;
                        updateMqttServerUri(mqttUri);
                    } else {
                        Toast.makeText(this, "Invalid input. Please try again.", Toast.LENGTH_SHORT).show();
                        showMqttConfigDialog();  // Reopen dialog if input is invalid
                    }
                })
                .show();
    }

    private void initializeMqttManager(String mqttUri) {
        mqttManager = new MqttManager(mqttUri, MQTT_CLIENT_ID);
        connectToMqttBroker();
    }

    private void updateMqttServerUri(String newUri) {
        try {


            if (mqttManager == null) {
                initializeMqttManager(newUri);
            } else {
                mqttManager.disconnect();
                mqttManager = new MqttManager(newUri, MQTT_CLIENT_ID);
                connectToMqttBroker();
                Toast.makeText(this, "MQTT Server Updated", Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception e){
            new AlertDialog.Builder(this)
                    .setTitle("Mqqt connection error")
                    .setMessage("Error connecting to server")
                    .setPositiveButton("Retry", (dialog, which) -> showMqttConfigDialog())
                    .show();
        }
    }

    private void toggleLocationMode() {
        locationService.toggleLocationMode(this);
    }
    private void showExitConfirmation() {       //Otan pataw Exit
        new AlertDialog.Builder(this)
                .setTitle("Exit Application")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> exitApp())
                .setNegativeButton("No", null)
                .show();
    }
    private void exitApp() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        mqttManager.disconnect();
        finishAffinity();  // Close all activities
        System.exit(0);    // Kill process
    }
}
