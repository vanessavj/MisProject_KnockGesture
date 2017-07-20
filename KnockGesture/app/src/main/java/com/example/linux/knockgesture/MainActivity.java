package com.example.linux.knockgesture;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.provider.MediaStore;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity{

    protected Map <String, Knock> gestureMap = new HashMap<String, Knock>();
    protected Button recordGesture;
    protected Button executeGesture;
    protected Button resetGestures;
    protected EditText enterName;
    protected TextView detectedGesture;
    protected TextView meansView;
    protected TextView meansSimilar;
    protected TextView recordGestureText;
    private SensorManager manager;
    private Sensor accel;
    private float[] gravity = new float[3];
    private float gravity_fac = 1.0f;//1.0f / 9.81f;
    private int fft_num_elements = 330;
    private int fft_size = 32;
    private double[] fft_x;
    private double[] fft_y;
    private double[] glob_array;
    private int fft_i = 0;
    private int fft_num_chunk = 0;
    List<List<Float>> accelData;
    private double threshold = 2;
    private long startTime;
    private boolean recording = false;
    private int async_num = 0;
    private boolean isExecuting = false;
    private boolean startRecording = false;
    final int mdelay = 100;

    private MediaPlayer mp;
    private final Uri song_uri = Uri.parse("/Music/song.m4a");

    private int counter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accel = manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        accelData = new ArrayList<List<Float>>(4);
        fft_x = new double[fft_num_elements];
        fft_y = new double[fft_num_elements];
        //Log.i("CEIL ", "" + (int) Math.ceil(fft_num_elements / (float)fft_size));
        glob_array = new double[(int) Math.ceil(fft_num_elements / (float)fft_size)];

        recordGesture = (Button) findViewById(R.id.recordGesture);
        executeGesture = (Button) findViewById(R.id.executeGesture);
        resetGestures = (Button) findViewById(R.id.reset);
        enterName = (EditText) findViewById(R.id.enterName);
        detectedGesture = (TextView) findViewById(R.id.detectedGestureText);
        meansView = (TextView) findViewById(R.id.meansView);
        meansSimilar = (TextView) findViewById(R.id.meansSimilar);
        //recordGestureText = (TextView) findViewById(R.id.recordGestureText);

        recordGesture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //recordAccel(view);
                isExecuting = false;
                recordGesture.setEnabled(false);
                startRecording = true;


            }
        });

        executeGesture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isExecuting = true;
                executeGesture.setEnabled(false);
                startRecording = true;

                for(String k : gestureMap.keySet()){
                    gestureMap.get(k).print();
                }
            }
        });

        resetGestures.setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View view) {
               gestureMap.clear();
           }
        });

        accelData = new ArrayList<List<Float>>(4);
        for(int i = 0; i < 4; i++) {
                accelData.add(new ArrayList<Float>());
            }

            manager.registerListener(new SensorEventListener() {

                @Override
                public void onSensorChanged(SensorEvent sensorEvent) {
                    recordGesture(sensorEvent);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int i) {

                }
            }, accel, SensorManager.SENSOR_DELAY_FASTEST);


        gestureMap.put("PLAY", new Knock("PLAY", new double[]{0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0}));
        gestureMap.put("NEXT", new Knock("NEXT", new double[]{0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0}));
        //gestureMap.put("STOP", new Knock("STOP", new double[]{0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0}));
        gestureMap.put("STOP", new Knock("STOP", new double[]{0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0}));

//            mp = new MediaPlayer();
//            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
//        try {
//            mp.setDataSource(getApplicationContext(), song_uri);
//            mp.prepareAsync();
//        } catch (IOException e) {
//            Log.i("ERROR", "FILE");
//            e.printStackTrace();
//        }
//
//        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
//
//            @Override
//            public void onPrepared(MediaPlayer mediaPlayer) {
//                mediaPlayer.start();
//            }
//        });

    }


    //private void recordGesture(){}

    private void recordGesture(SensorEvent sensorEvent){
        if(startRecording){
            startRecording = false;
            recording = true;
            fft_x = new double[fft_num_elements];
            fft_y = new double[fft_num_elements];
            fft_i = 0;
            recording = true;
            counter = 0;
            startTime = SystemClock.uptimeMillis();
        }
        if(recording){

            long curr_time = SystemClock.uptimeMillis();
            if(curr_time - startTime < 3000 + mdelay){
                if(curr_time - startTime > mdelay) {
                    if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                        // float[] tmp = isolate_gravity(sensorEvent.values);
                        float[] tmp = sensorEvent.values;
                        accelData.get(0).add(tmp[0]);
                        accelData.get(1).add(tmp[1]);
                        accelData.get(2).add(tmp[2]);
                        Log.i("Sensordata: ", tmp[0] + "  " + tmp[1] + "  " + tmp[2]);
                        float mag = (float) Math.sqrt(tmp[0] * tmp[0] + tmp[1] * tmp[1] + tmp[2] * tmp[2]);
                        Log.i("Magnitude", mag + " ");
                        accelData.get(3).add(mag);
                        fft_x[fft_i] = mag;
                        fft_i = (fft_i + 1)%fft_num_elements;
                        counter++;
                    }
                }
            }
            else{
                recording = false;

                if(isExecuting){
                    executeGesture.setEnabled(true);
                }else{
                    recordGesture.setEnabled(true);
                }
                Log.i("fft_x", array_to_string(fft_x));
                new calc_fft().execute(new FFT_async_type(fft_x, fft_y));
                fft_i = 0;
                Log.i("num_elem: ", counter+"");

                //gestureMap.put(enterName.getText().toString(), new Knock(enterName.getText().toString()));


            }
        }

    }

    private String array_to_string(double[] doubles){
        String out = "";
        for(double d : doubles){
            out += d + ", ";
        }
        return out;
    }

    private String array_to_awesome_string(double[] doubles){
        String out = "";
        for(double d : doubles){
            if(d == 1.0){
                out += "\u25A0";
            }else{
                out += "\u25A1";
            }
        }
        return out;
    }


    private float[] isolate_gravity(float[] f){

        gravity[0] = 0.8f * gravity[0] + 0.2f * f[0];
        gravity[1] = 0.8f * gravity[1] + 0.2f * f[1];
        gravity[2] = 0.8f * gravity[2] + 0.2f * f[2];

        float[] ret = new float[3];

        ret[0] = f[0] - gravity[0];
        ret[1] = f[1] - gravity[1];
        ret[2] = f[2] - gravity[2];

        return ret;
        //return f;
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
                if(data[i] < 1.2){
//                if(data[i] < 0.25 * peak){
                    data[i] = 0;
                } else{
                    data[i] = 1;
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
            int num_chunks = (int) Math.ceil(fft_num_elements / (float)fft_size);
            double[] ret = new double[num_chunks];
            FFT fourier = new FFT(fft_size);
            for(int i = 0; i < num_chunks; i++){
                double[] tmp1 = Arrays.copyOfRange(raw[0].fftx, i*fft_size, (i+1) * fft_size);;
                double[] tmp2 = new double[fft_size];
                Log.i("FFT INPUT: ", array_to_string(tmp1));

//                fourier.fft(tmp1, tmp2);

                double mean = 0;
                for(int j = 0; j < fft_size; j++){
                    mean += tmp1[i];
//                    mean += absolute(tmp1[i], tmp2[i]);
                }
                mean /= (float)fft_size;
                Log.i("mean: ", mean+"");
                ret[i] = mean;
            }

//            return ret;
            return normalize_vec(ret);
        }

        protected void onPostExecute(double[] input){
            glob_array = input;
            Log.i("debug", "Means " + array_to_string(glob_array));

            float similar = Float.MAX_VALUE;
            String name = "";
            for(String k : gestureMap.keySet()){
                float tmp = isSimilar(gestureMap.get(k).means, glob_array);
                Log.i("debug", "similarity score of " + k +" is: " + tmp);
                if(tmp < similar){
                    similar = tmp;
                    name = k;
                }

            }

            if(!isExecuting){
                if(similar <= 1 || hammingweight(input) <= 1){
                    detectedGesture.setText("Gesture too similar to one that is already stored, please try again");
                }else {
                    detectedGesture.setText("Gesture stored");
                    String input_name = enterName.getText().toString();
                    gestureMap.put(input_name, new Knock(input_name, glob_array));
                    meansView.setText("Input: \n" + array_to_awesome_string(glob_array));
                    meansSimilar.setText("");
                    enterName.setText("");

                }
            } else {

                if(similar >= 60){
                    detectedGesture.setText("Stored Knocks not similar enough (Similar:" + similar + ")");
                    meansView.setText("Input: \n" + array_to_awesome_string(glob_array));
                    meansSimilar.setText("");
                } else {
                    Log.i("Similar:", name + "  " + similar);
                    detectedGesture.setText("Similar: " + name + "  " + similar);
                    meansView.setText("Input: \n" + array_to_awesome_string(glob_array));
                    meansSimilar.setText("Detected Gesture: \n" + array_to_awesome_string(gestureMap.get(name).means));
                }
            }
        }
    }






    private List<Integer> get_list_without_i(List<Integer> list, int idx){
        List l = new ArrayList<Integer>();
        for(int i = 0; i < list.size(); i++) {
            if (i != idx) {
                l.add(list.get(i));
            }
        }
        return l;
    }

    //p1 is gesture, p2 input

    private int isSimilar( double[] p1, double[] p2) {
        //Log.i("Debug ", "is_similar has been called");
        List xor = new ArrayList();
        Log.i("Debug", "p1 " + array_to_string(p1));
        for (int i = 0; i < p1.length; i++){
            xor.add((int) p1[i] ^ (int) p2[i]);
        }
        int k = 2 * hammingweight(xor);
        if (k == 0){
            return 0;
        }
        Log.i("Debug", "k = " + k);
        List p1_indices = getIndices(p1);
        List p2_indices = getIndices(p2);

        if(p2_indices.size() == 0){
            return Integer.MAX_VALUE;
        }

        int p = 1;
        int q = 1;
        for (int i = 0; i < p2_indices.size() - 1; i++){
            p *= sigmoid((int) p2_indices.get(i+1) - (int) p2_indices.get(i));
        }
        for (int i = 0; i < p1_indices.size() - 1; i++){
            q *= sigmoid((int) p1_indices.get(i+1) - (int) p1_indices.get(i));
        }

        p = Math.abs(p - q) + 1;

        Log.i("Debug", " p = " + p);

        // hammingweight of gesture ( p1)
        //Anpassen, damit viele Knocks nicht zu sehr gepunished werden
        int h = (int) 0.75 * p1_indices.size();
        p += Math.abs((int) p1_indices.get(0) - (int) p2_indices.get(0));

        Log.i("Debug", "h = " + h);
        Log.i("Debug ", "is_similar returns " + (k * p + h));
        return k * p + h;
    }

    private int sigmoid(int distance){
        if (distance <= 1){
            return 1;
        }
        else{
            return 2;
        }
    }

    private int hammingweight(List xor){
        int hamming = 0;
        for (int i = 0; i < xor.size(); i++){
            hamming += (int) xor.get(i);
        }
        return hamming;
    }

    private int hammingweight(double[] xor){
        int hamming = 0;
        for (int i = 0; i < xor.length; i++){
            hamming += (int) xor[i];
        }
        return hamming;
    }


    private List<Integer> getIndices(double[] p) {
        List list = new ArrayList<Integer>();
        for(int i = 0; i< p.length; i++){
            if (p[i] != 0){
                list.add(i);
            }
        }
        return list;
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
