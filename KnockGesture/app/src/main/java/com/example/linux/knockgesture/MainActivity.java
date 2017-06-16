package com.example.linux.knockgesture;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v4.text.TextUtilsCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
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
    private float gravity_fac = 1.0f;//1.0f / 9.81f;
    private int fft_num_elements = 448;
    private double[] fft_x;
    private double[] fft_y;
    private double[] glob_array;
    private int fft_i = 0;
    private int fft_num_chunk = 0;
    List<List<Float>> accelData;
    private double threshold = 0.05;
    private long startTime;
    private boolean recording = false;
    private int async_num = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accel = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accelData = new ArrayList<List<Float>>(4);
        fft_x = new double[fft_num_elements];
        fft_y = new double[fft_num_elements];

        glob_array = new double[14];

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
                                    Log.i("Sensordata: ", tmp[0] + "  " + tmp[1] + "  " + tmp[2]);
                                    float mag = (float) Math.sqrt(tmp[0] * tmp[0] + tmp[1] * tmp[1] + tmp[2] * tmp[2]);
                                    Log.i("Magnitude", mag + " ");
                                    accelData.get(3).add(mag);
                                    fft_x[fft_i] = mag;
                                    fft_i = (fft_i + 1) % fft_num_elements;
                                }
                            }
                            else{
                                recording = false;
                                manager.unregisterListener(this);
                                recordGesture.setEnabled(true);
                                Log.i("fft_x", array_to_string(fft_x));
                                new calc_fft().execute(new FFT_async_type(fft_x, fft_y));

                                //gestureMap.put(enterName.getText().toString(), new Knock(enterName.getText().toString()));


                            }
                        }
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int i) {

                    }
                }, accel, SensorManager.SENSOR_DELAY_FASTEST);


            }
        });

        executeGesture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(String k : gestureMap.keySet()){
                    gestureMap.get(k).print();
                }
            }
        });

    }

    private String array_to_string(double[] doubles){
        String out = "";
        for(double d : doubles){
            out += d + ", ";
        }
        return out;
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

    private static double absolute(double mx, double my){
        return Math.sqrt(mx * mx + my * my);
    }

    private double[] normalize_vec(double[] data){
        double peak = 0;
        for(double d : data){
               peak = Math.max(peak, d);
        }

        if(peak > threshold){
            for(int i = 0; i < data.length; i++){
                if(data[i] < 0.5 * peak){
                    data[i] = 0;
                } else{
                    data[i] = data[i] / peak * 100;
                }
            }
        }else{
            data = new double[data.length];
        }
        return data;
    }


    private class calc_fft extends AsyncTask<FFT_async_type, double[], double[]> {

        protected double[] doInBackground(FFT_async_type... raw){
            Log.i("TASK INPUT: ", array_to_string(raw[0].fftx));
            int num_chunks = (int) Math.ceil(fft_num_elements / 32.0);
            double[] ret = new double[num_chunks];
            FFT fourier = new FFT(32);
            for(int i = 0; i < num_chunks; i++){
                double[] tmp1 = Arrays.copyOfRange(raw[0].fftx, i*32, (i+1) * 32);;
                double[] tmp2 = new double[32];
                Log.i("FFT INPUT: ", array_to_string(tmp1));

                fourier.fft(tmp1, tmp2);

                double mean = 0;
                for(int j = 0; j < 32; j++){
                    mean += absolute(tmp1[i], tmp2[i]);
                }
                mean /= 32.0;
                Log.i("mean: ", mean+"");
                ret[i] = mean;
            }

//            return ret;
            return normalize_vec(ret);
        }

        protected void onPostExecute(double[] input){
            glob_array = input;
            Log.i("Means", array_to_string(glob_array));
            String name = enterName.getText().toString();
            gestureMap.put(name, new Knock(name, glob_array));
            enterName.setText("");


        }
    }

    private class FFT_async_type {
        public double[] fftx;
        public double[] ffty;

        FFT_async_type(double[] mfftx, double[] mffty){
            fftx = mfftx;
            ffty = mffty;
        }
    }

}
