package com.example.IotDevice2App;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.IotDevice2App.models.Sensor;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.List;

public class SensorSliderFragment extends Fragment {

    private static final String ARG_SENSORS = "sensors";
    private List<Sensor> sensors;

    public static SensorSliderFragment newInstance(ArrayList<Sensor> sensors) {
        SensorSliderFragment fragment = new SensorSliderFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SENSORS, sensors);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            sensors = (List<Sensor>) getArguments().getSerializable(ARG_SENSORS);
        } else {
            sensors = new ArrayList<>();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sensor_slider, container, false);

        // Find the container for sliders
        LinearLayout sliderContainer = view.findViewById(R.id.sliderContainer);

        // Create sliders gia ta arxika sensors
        for (Sensor sensor : sensors) {
            addSensorToContainer(sliderContainer, sensor);
        }

        return view;
    }

    public void addSensor(Sensor newSensor) {
        sensors.add(newSensor);
        if (getView() != null) {
            LinearLayout sliderContainer = getView().findViewById(R.id.sliderContainer);
            addSensorToContainer(sliderContainer, newSensor);
        }
    }

    // Helper method gia sensor Ui
    private void addSensorToContainer(LinearLayout sliderContainer, Sensor sensor) {

        TextView sensorTypeText = new TextView(getContext());   //Text gia to type tou sensor
        sensorTypeText.setText("Sensor: " + sensor.getType());
        sensorTypeText.setTextSize(18);
        sliderContainer.addView(sensorTypeText);

        Switch sensorSwitch = new Switch(getContext()); //Switch gia apostolh h mh apostolh data se server
        sensorSwitch.setChecked(sensor.isEnabled());
        sliderContainer.addView(sensorSwitch);

        Slider sensorSlider = new Slider(getContext()); //Dhmiourgia Slider gia sensor
        sensorSlider.setValueFrom((float) sensor.getMinValue());
        sensorSlider.setValueTo((float) sensor.getMaxValue());
        sensorSlider.setValue((float) sensor.getCurrentValue());
        sliderContainer.addView(sensorSlider);

        TextView sensorValueText = new TextView(getContext());  //Textview gia Value
        sensorValueText.setText("Value: " + sensor.getCurrentValue());
        sliderContainer.addView(sensorValueText);

        sensorSlider.addOnChangeListener((slider, value, fromUser) -> {     //Update value otan allazei to slider
            sensor.setCurrentValue(value);
            sensorValueText.setText("Value: " + String.format("%.2f", value));
        });

        sensorSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> sensor.setEnabled(isChecked));   //Diaxeirisei trigger sto switch
    }

    public List<Sensor> getCurrentSensorValues() {
        return sensors;
    }
}
