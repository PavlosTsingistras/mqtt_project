package com.test.androidapp.mymqttapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import com.test.androidapp.R;


import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class LocationService {

    public interface LocationCallback {
        void onResult(double lat, double lon, String error);
    }

    private final Context context;
    private final FusedLocationProviderClient fusedClient;
    private final CancellationTokenSource cancellationTokenSource;
    private boolean manualMode = false;
    private List<XmlLocationParser.LocationVector> manualLocations;
    private int manualIndex = 0;

    private MediaPlayer moderatePlayer, highPlayer;

    public LocationService(Context context) {
        this.context = context;
        this.fusedClient = LocationServices.getFusedLocationProviderClient(context);
        this.cancellationTokenSource = new CancellationTokenSource();
        this.manualLocations = new ArrayList<>();
        initMediaPlayers();
    }

    private void initMediaPlayers() {
        moderatePlayer = MediaPlayer.create(context, R.raw.moderate_alert);
        highPlayer = MediaPlayer.create(context, R.raw.high_alert);
    }

    public void setManualMode(boolean isManual) {
        this.manualMode = isManual;
    }
    public void loadManualLocations(String fileName) {
        try {
            manualLocations = XmlLocationParser.parseXmlFile(context.getAssets(), fileName);
            manualIndex = 0;
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    public void fetchNextLocation(LocationCallback callback) {
        if (manualMode) {
            fetchManualLocation(callback);
        } else {
            fetchLiveLocation(callback);
        }
    }

    private void fetchManualLocation(LocationCallback callback) {
        if (manualLocations.isEmpty()) {
            callback.onResult(0, 0, "No manual data loaded");
            return;
        }
        XmlLocationParser.LocationVector vec = manualLocations.get(manualIndex);
        manualIndex = (manualIndex + 1) % manualLocations.size();
        callback.onResult(vec.latitude, vec.longitude, null);
    }

    private void fetchLiveLocation(LocationCallback callback) {
        // vlepoume an einai granted to permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            callback.onResult(0, 0, "Location permission not granted");
            return;
        }
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        callback.onResult(location.getLatitude(), location.getLongitude(), null);
                    } else {
                        callback.onResult(0, 0, "Location is null");
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onResult(0, 0, "Location failure: " + e.getMessage());
                });
    }

    public void playHighAlert() {
        if (highPlayer != null) {
            highPlayer.start();
        }
    }

    public void playModerateAlert() {
        if (moderatePlayer != null) {
            moderatePlayer.start();
        }
    }

    public boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
                return capabilities != null &&
                        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            }
        }
        return false;
    }

    public void release() {
        if (moderatePlayer != null) {
            moderatePlayer.release();
        }
        if (highPlayer != null) {
            highPlayer.release();
        }
    }
}
