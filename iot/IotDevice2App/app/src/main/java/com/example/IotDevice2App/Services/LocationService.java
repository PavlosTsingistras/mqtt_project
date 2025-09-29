package com.example.IotDevice2App.Services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.LocationRequest.Builder;

public class LocationService {
    private static LocationService instance;
    private final Context context;
    private Location currentLocation;
    private boolean isManualMode;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private LocationService(Context context) {
        this.context = context.getApplicationContext();
        this.isManualMode = false; // Default to automatic mode
        this.currentLocation = new Location(0.0, 0.0); // Default location
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        initLocationUpdates();
    }

    public static LocationService getInstance(Context context) {
        if (instance == null) {
            synchronized (LocationService.class) {
                if (instance == null) {
                    instance = new LocationService(context);
                }
            }
        }
        return instance;
    }

    public boolean isManualMode() {
        return isManualMode;
    }

    public void setManualMode(boolean manualMode) {
        this.isManualMode = manualMode;
    }

    public Location getCurrentLocation() {
        return isManualMode ? currentLocation : getAutomaticLocation();
    }

    public void setManualLocation(double latitude, double longitude) {
        this.currentLocation = new Location(latitude, longitude);
    }

    @SuppressLint("MissingPermission")
    private Location getAutomaticLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("LocationService", "Location permission not granted.");
            return new Location(0.0, 0.0);
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLocation = new Location(location.getLatitude(), location.getLongitude());
                    } else {
                        Log.e("LocationService", "Last known location is null.");
                    }
                })
                .addOnFailureListener(e -> Log.e("LocationService", "Error getting location", e));

        return currentLocation;
    }

    private void initLocationUpdates() {
        LocationRequest locationRequest = new Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateIntervalMillis(1000) //  updates every second
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                currentLocation = new Location(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
            }
        };
    }

    public void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(new Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                    .setMinUpdateIntervalMillis(1000)       //every second updates
                    .build(), locationCallback, null);
        }
    }

    public void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    public void toggleLocationMode(Context activityContext) {
        isManualMode = !isManualMode;
        if (isManualMode) {
            promptForManualLocation(activityContext);
        }
        Toast.makeText(activityContext, "Location Mode: " + (isManualMode ? "Manual" : "Automatic"), Toast.LENGTH_SHORT).show();
    }

    private void promptForManualLocation(Context activityContext) {
        LayoutInflater inflater = LayoutInflater.from(activityContext);
        android.view.View dialogView = inflater.inflate(com.example.IotDevice2App.R.layout.dialog_set_location, null);
        EditText latInput = dialogView.findViewById(com.example.IotDevice2App.R.id.latInput);
        EditText lonInput = dialogView.findViewById(com.example.IotDevice2App.R.id.lonInput);

        new AlertDialog.Builder(activityContext)
                .setTitle("Set Manual Location")
                .setView(dialogView)
                .setPositiveButton("Set", (dialog, which) -> {
                    try {
                        double latitude = Double.parseDouble(latInput.getText().toString().trim());
                        double longitude = Double.parseDouble(lonInput.getText().toString().trim());
                        setManualLocation(latitude, longitude);
                    } catch (NumberFormatException e) {
                        Toast.makeText(activityContext, "Invalid latitude/longitude", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
