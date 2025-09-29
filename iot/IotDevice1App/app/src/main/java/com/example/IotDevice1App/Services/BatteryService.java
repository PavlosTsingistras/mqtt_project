package com.example.IotDevice1App.Services;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class BatteryService {
    private static BatteryService instance;
    private final Context context;

    private BatteryService(Context context) {
        this.context = context.getApplicationContext();
    }

    public static BatteryService getInstance(Context context) {
        if (instance == null) {
            synchronized (BatteryService.class) {
                if (instance == null) {
                    instance = new BatteryService(context);
                }
            }
        }
        return instance;
    }

    public int getBatteryPercentage() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            return (int) ((level / (float) scale) * 100);
        }
        return -1; // Return -1 an error
    }
}
