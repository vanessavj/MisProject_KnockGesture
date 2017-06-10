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
    private int fft_num_elements = 32;
    private double[][] fft_x;
    private double[][] fft_y;
    private double[] glob_array;
    private int fft_i = 0;
    private int fft_num_chunk = 0;
    List<List<Float>> accelData;
    private double t = 0;
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
        fft_x = new double[14][fft_num_elements];
        fft_y = new double[14][fft_num_elements];

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
                fft_x = new double[14][fft_num_elements];
                fft_y = new double[14][fft_num_elements];
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
                                    fft_x[fft_num_chunk][fft_i] = mag;
                                    fft_i = (fft_i + 1) % fft_num_elements;
                                    if(fft_i == 0){
                                        fft_num_chunk = (fft_num_chunk + 1) % 14;
                                    }
                                }
                            }
                            else{
                                recording = false;
                                manager.unregisterListener(this);
                                recordGesture.setEnabled(true);
                                for(int i = 0; i < 14; i++){
                                    Log.i("fft_x", array_to_string(fft_x[i]));
                                    new calc_fft().execute(new FFT_async_type(fft_x[i],fft_y[i], i));
                                }

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


    private class calc_fft extends AsyncTask<FFT_async_type, double[], Pair<Double, Integer>> {

        protected Pair<Double,Integer> doInBackground(FFT_async_type... raw){
            double[] tmp1 = raw[0].fftx;
            double[] tmp2 = raw[0].ffty;
            Log.i("FFT INPUT: ", array_to_string(tmp1));

            FFT fourier = new FFT(fft_num_elements);
            fourier.fft(tmp1, tmp2);

            double[] ret = new double[fft_num_elements];
            //double peak = 0;
            double mean = 0;
            for(int i = 1; i < fft_num_elements; i++){
                double tmp = absolute(tmp1[i], tmp2[i]);
                ret[i] = tmp;
                mean += tmp;
                //peak = Math.max(peak, tmp);
            }
            mean /= ret.length;
            /*
            if (peak >= t){
                String out = "";
                for(int i = 0; i < ret.length; i++){
                    if(ret[i] < 0.9 * peak){
                        ret[i] = 0;
                    } else{
                        ret[i] = ret[i] / peak * 100;
                    }
                    out += ret[i] + "  ";

                }
                Log.i("danach", out);
            }*/
            return new Pair<Double, Integer>(mean, raw[0].i) ;
        }

        protected void onPostExecute(Pair<Double, Integer> input){
           glob_array[input.second] = input.first;
            async_num += 1;
            if (async_num == 14){
                async_num = 0;
                Log.i("Means", array_to_string(glob_array));
                String name = enterName.getText().toString();
                gestureMap.put(name, new Knock(name, glob_array));
                enterName.setText("");
            }

        }
    }

    private class FFT_async_type {
        public double[] fftx;
        public double[] ffty;
        public int i;

        FFT_async_type(double[] mfftx, double[] mffty, int mi){
            fftx = mfftx;
            ffty = mffty;
            i = mi;
        }
    }

}
