package com.example.linux.knockgesture;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity{

    protected Map <String, Knock> gestureMap = new HashMap<String, Knock>();
    protected Button recordGesture;
    protected Button executeGesture;
    protected EditText enterName;
    protected TextView detectedGesture;
    private SensorManager manager;
    private Sensor accel;
    private float[] gravity = new float[3];
    private float gravity_fac = 1.0f / 9.81f;
    private int fft_num_elements = 512;
    private double[] fft_x;
    private double[] fft_y;
    private int fft_i = 0;
    List<List<Float>> accelData;
    private long startTime;
    private boolean recording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accel = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accelData = new ArrayList<List<Float>>(4);
        fft_x = new double[fft_num_elements];
        fft_y = new double[fft_num_elements];

        recordGesture = (Button) findViewById(R.id.recordGesture);
        executeGesture = (Button) findViewById(R.id.executeGesture);
        enterName = (EditText) findViewById(R.id.enterName);
        detectedGesture = (TextView) findViewById(R.id.detectedGestureText);

        recordGesture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //recordAccel(view);
                recordGesture.setEnabled(false);
                accelData = new ArrayList<List<Float>>(4);
                for(int i = 0; i < 4; i++) {
                    accelData.add(new ArrayList<Float>());
                }
                fft_x = new double[fft_num_elements];
                fft_y = new double[fft_num_elements];
                fft_i = 0;
                recording = true;
                startTime = SystemClock.uptimeMillis();
                manager.registerListener(new SensorEventListener() {

                    @Override
                    public void onSensorChanged(SensorEvent sensorEvent) {
                        if(recording){
                            if(SystemClock.uptimeMillis() - startTime < 2000){
                                if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                                    float[] tmp = isolate_gravity(sensorEvent.values);
                                    accelData.get(0).add(tmp[0]);
                                    accelData.get(1).add(tmp[1]);
                                    accelData.get(2).add(tmp[2]);
                                    float mag = (float) Math.sqrt(tmp[0] * tmp[0] + tmp[1] * tmp[1] + tmp[2] * tmp[2]);
                                    accelData.get(3).add(mag);
                                    fft_x[fft_i] = mag;
                                    fft_i = (fft_i + 1) % fft_num_elements;
                                }
                            }
                            else{
                                recording = false;
                                manager.unregisterListener(this);
                                recordGesture.setEnabled(true);
                                new calc_fft().execute(fft_x,fft_y);
                                gestureMap.put(enterName.getText().toString(), new Knock(enterName.getText().toString()));
                                enterName.setText("");
                                Log.i("Size", accelData.get(0).size()+" ");
                            }
                        }
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int i) {

                    }
                }, accel, SensorManager.SENSOR_DELAY_FASTEST);
                String name = enterName.getText().toString();
                gestureMap.put(name, new Knock(name));

            }
        });

        executeGesture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

    }

    private float[] isolate_gravity(float[] f){

        gravity[0] = 0.8f * gravity[0] + 0.2f * f[0];
        gravity[1] = 0.8f * gravity[1] + 0.2f * f[1];
        gravity[2] = 0.8f * gravity[2] + 0.2f * f[2];

        float[] ret = new float[3];

        ret[0] = (f[0] - gravity[0]) * gravity_fac;
        ret[1] = (f[1] - gravity[1]) * gravity_fac;
        ret[2] = (f[2] - gravity[2]) * gravity_fac;

        return ret;
    }

    private static float absolute(double mx, double my){
        return (float) Math.sqrt(mx * mx + my * my);
    }


    private class calc_fft extends AsyncTask<double[], double[], float[]> {

        protected float[] doInBackground(double[]... raw){
            double[] tmp1 = raw[0];
            double[] tmp2 = raw[1];
            Log.i("davort", raw[0] + "");

            FFT fourier = new FFT(fft_num_elements);
            fourier.fft(tmp1, tmp2);
            Log.i("danach", raw[0] + "");
            float[] ret = new float[fft_num_elements];
            for(int i = 0; i < fft_num_elements; i++){
                ret[i] = absolute(tmp1[i], tmp2[i]);
            }
            return ret;
        }
    }
}
