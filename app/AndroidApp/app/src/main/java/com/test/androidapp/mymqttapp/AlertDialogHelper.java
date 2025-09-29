package com.test.androidapp.mymqttapp;

import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class AlertDialogHelper {
    private AlertDialog currentDialog;
    private TextView alertTextView;


    public void displayAlert(AppCompatActivity activity, String risk, int distance) {
        int bgColor = "HIGH".equalsIgnoreCase(risk) ? 0xFFFFCDD2 : 0xFFFFF9C4;
        String message = "Risk Level: " + risk + "\nDistance: " + distance + " meters";

        if (currentDialog != null && currentDialog.isShowing() && alertTextView != null) {
            alertTextView.setText(message);
            alertTextView.setBackgroundColor(bgColor);
        } else {
            alertTextView = new TextView(activity);
            alertTextView.setText(message);
            alertTextView.setPadding(50, 50, 50, 50);
            alertTextView.setBackgroundColor(bgColor);
            currentDialog = new AlertDialog.Builder(activity)
                    .setView(alertTextView)
                    .setPositiveButton("OK", (dialog, which) -> currentDialog = null)
                    .show();
        }
    }
}
