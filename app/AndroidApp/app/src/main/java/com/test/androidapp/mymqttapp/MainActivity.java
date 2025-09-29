package com.test.androidapp.mymqttapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.test.androidapp.mymqttapp.models.Alert;
import com.test.androidapp.mymqttapp.models.LocationData;
import com.test.androidapp.R;
import com.google.gson.Gson;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String CLIENT_ID = "AndroidDevice";
    private static final String PUBLISH_TOPIC = "android/location";
    private static final String SUBSCRIBE_TOPIC = "android/notifications";

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private TextView connectionStatus;
    private Button startButton, stopButton;
    private Switch modeSwitch; // OFF = Automatic; ON = Manual.

    private MqttManager mqttManager;
    private boolean isTransmitting = false;
    private ScheduledExecutorService scheduler;
    private Gson gson = new Gson();

    private int transmissionDuration = 0; // seconds; 0 = continuous
    private int transmissionCount = 0;

    private String lastAlertRisk = "";
    private LocationService locationService;

    private AlertDialogHelper alertDialogHelper = new AlertDialogHelper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
        initLocationService();
        initMqtt();
        startConnectivityCheck();
    }

    private void initUI() {
        connectionStatus = findViewById(R.id.textConnectionStatus);
        startButton = findViewById(R.id.buttonStart);
        stopButton = findViewById(R.id.buttonStop);
        modeSwitch = findViewById(R.id.switchMode);

        startButton.setOnClickListener(v -> startTransmission());
        stopButton.setOnClickListener(v -> stopTransmission());

        modeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            locationService.setManualMode(isChecked);
            if (isChecked) {
                String fileName = new Random().nextBoolean() ? "android_1.xml" : "android_2.xml";
                locationService.loadManualLocations(fileName);
                Toast.makeText(MainActivity.this, "Switched to Manual Mode", Toast.LENGTH_SHORT).show();
            } else {
                transmissionDuration = 0;
                Toast.makeText(MainActivity.this, "Switched to Automatic Mode ", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void initLocationService() {
        if (checkLocationPermission()) {
            locationService = new LocationService(this);
        } else {
            requestLocationPermission();
        }
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationService = new LocationService(this);
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initMqtt() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String serverIp = prefs.getString("server_ip", "0.0.0");
        int serverPort = prefs.getInt("server_port", 0);
        String serverUri = "tcp://" + serverIp + ":" + serverPort;

        if (mqttManager != null) {      //An arxisw kainourgio connection disconect to palio
            mqttManager.disconnect();
        }

        mqttManager = new MqttManager(serverUri, CLIENT_ID);
        mqttManager.connect(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                connectionStatus.setText("Connected to MQTT Broker");
                subscribeToAlerts();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                connectionStatus.setText("Failed to connect to MQTT");
                Log.e(TAG, "MQTT connection failed", exception);
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Invalid IP address or server unreachable", Toast.LENGTH_LONG).show()
                );
                mqttManager.disconnect();
            }
        });
    }



    private void subscribeToAlerts() {
        mqttManager.subscribe(SUBSCRIBE_TOPIC, 1, (topic, message) -> {
            String payload = new String(message.getPayload());
            Log.d(TAG, "Received alert JSON: " + payload);
            System.out.println("Received alert JSON: " + payload);
            runOnUiThread(() -> handleAlert(payload));
        });
    }

    private void startTransmission() {
        if (isTransmitting) return;
        isTransmitting = true;
        transmissionCount = 0;

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(() -> {
            if (!isTransmitting) return;
            if (transmissionDuration > 0 && transmissionCount >= transmissionDuration) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Transmission complete", Toast.LENGTH_SHORT).show());
                stopTransmission();
                return;
            }
            locationService.fetchNextLocation((lat, lon, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error fetching location: " + error);
                    return;
                }
                publishLocation(lat, lon);
                transmissionCount++;
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void stopTransmission() {
        isTransmitting = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void publishLocation(double latitude, double longitude) {
        LocationData data = new LocationData(latitude, longitude, 10);
        String payload = gson.toJson(data);
        mqttManager.publish(PUBLISH_TOPIC, payload);
    }


    private void handleAlert(String json) {
        try {
            Alert alert = gson.fromJson(json, Alert.class);
            if (alert == null) return;
            String risk = alert.getRiskLevel();
            int distance = alert.getDistance();
            alertDialogHelper.displayAlert(this, risk, distance);

            // Play sound mono otan allazei to risk
            if (!risk.equalsIgnoreCase(lastAlertRisk)) {
                if ("HIGH".equalsIgnoreCase(risk)) {
                    locationService.playHighAlert();
                } else {
                    locationService.playModerateAlert();
                }
                lastAlertRisk = risk;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse alert: " + e.getMessage(), e);
        }
    }

    private void startConnectivityCheck() {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable checkTask = new Runnable() {
            @Override
            public void run() {
                if (!locationService.isInternetAvailable()) {
                    Toast.makeText(MainActivity.this, "No Internet connection", Toast.LENGTH_LONG).show();
                }
                handler.postDelayed(this, 10000);
            }
        };
        handler.post(checkTask);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void showMqttConfigDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("MQTT Configuration");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        EditText inputIp = new EditText(this);
        inputIp.setHint("Enter MQTT Server IP");
        inputIp.setInputType(InputType.TYPE_CLASS_TEXT);

        EditText inputPort = new EditText(this);
        inputPort.setHint("Enter MQTT Port");
        inputPort.setInputType(InputType.TYPE_CLASS_NUMBER);


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        inputIp.setText(prefs.getString("server_ip", "0"));
        inputPort.setText(String.valueOf(prefs.getInt("server_port", 0)));

        layout.addView(inputIp);
        layout.addView(inputPort);
        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String serverIp = inputIp.getText().toString().trim();
            int serverPort;
            try {
                serverPort = Integer.parseInt(inputPort.getText().toString().trim());
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid port number", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("server_ip", serverIp);
            editor.putInt("server_port", serverPort);
            editor.apply();

            Toast.makeText(this, "MQTT settings saved", Toast.LENGTH_SHORT).show();

            initMqtt();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_configure_mqtt) {
            showMqttConfigDialog();
            return true;
        } if (id == R.id.action_exit) {
            showExitConfirmation();
            return true;
        }else if (id == R.id.action_transmission_duration) {
            showTransmissionDurationDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showTransmissionDurationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Transmission Duration");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        EditText inputDuration = new EditText(this);
        inputDuration.setHint("Enter duration in seconds (0 = continuous)");
        inputDuration.setInputType(InputType.TYPE_CLASS_NUMBER);

        inputDuration.setText(String.valueOf(transmissionDuration));

        layout.addView(inputDuration);
        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            int newDuration;
            try {
                newDuration = Integer.parseInt(inputDuration.getText().toString().trim());
            } catch (NumberFormatException e) {
                Toast.makeText(MainActivity.this, "Invalid duration value", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update the transmissionDuration variable
            transmissionDuration = newDuration;
            Toast.makeText(MainActivity.this, "Transmission duration set to "
                    + newDuration + " seconds", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
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
        finishAffinity();
        System.exit(0);    // Kill
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTransmission();
        mqttManager.disconnect();
        locationService.release();
    }
}
