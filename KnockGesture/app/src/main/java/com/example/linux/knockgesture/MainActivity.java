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
    protected Button resetGestures;
    protected EditText enterName;
    protected TextView detectedGesture;
    private SensorManager manager;
    private Sensor accel;
    private float[] gravity = new float[3];
    private float gravity_fac = 1.0f;//1.0f / 9.81f;
    private int fft_num_elements = 700;
    private int fft_size = 64;
    private double[] fft_x;
    private double[] fft_y;
    private double[] glob_array;
    private int fft_i = 0;
    private int fft_num_chunk = 0;
    List<List<Float>> accelData;
    private double threshold = 0.5;
    private long startTime;
    private boolean recording = false;
    private int async_num = 0;
    private boolean isExecuting = false;

    private int counter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accel = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
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

        recordGesture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //recordAccel(view);
                isExecuting = false;
                recordGesture.setEnabled(false);
                recordGesture();


            }
        });

        executeGesture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isExecuting = true;
                executeGesture.setEnabled(false);
                recordGesture();

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

    }

    /*TODO:
    * Input sanitation
    *  Knock Number Tolerance
    * */

    private void recordGesture(){
        accelData = new ArrayList<List<Float>>(4);
        for(int i = 0; i < 4; i++) {
            accelData.add(new ArrayList<Float>());
        }
        fft_x = new double[fft_num_elements];
        fft_y = new double[fft_num_elements];
        fft_i = 0;
        recording = true;
        counter = 0;
        startTime = SystemClock.uptimeMillis();
        final int mdelay = 100;
        manager.registerListener(new SensorEventListener() {

            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if(recording){
                    long curr_time = SystemClock.uptimeMillis();
                    if(curr_time - startTime < 3000 + mdelay){
                        if(curr_time - startTime > mdelay) {
                            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                                float[] tmp = isolate_gravity(sensorEvent.values);
//                                float[] tmp = sensorEvent.values;
                                accelData.get(0).add(tmp[0]);
                                accelData.get(1).add(tmp[1]);
                                accelData.get(2).add(tmp[2]);
                                Log.i("Sensordata: ", tmp[0] + "  " + tmp[1] + "  " + tmp[2]);
                                float mag = (float) Math.sqrt(tmp[0] * tmp[0] + tmp[1] * tmp[1] + tmp[2] * tmp[2]);
                                Log.i("Magnitude", mag + " ");
                                accelData.get(3).add(mag);
                                fft_x[fft_i] = mag;
                                fft_i = (fft_i + 1);
                                counter++;
                            }
                        }
                    }
                    else{
                        recording = false;
                        manager.unregisterListener(this);
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

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        }, accel, SensorManager.SENSOR_DELAY_FASTEST);
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
                if(data[i] < 0.4 * peak){
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
            int num_chunks = (int) Math.ceil(fft_num_elements / (float)fft_size);
            double[] ret = new double[num_chunks];
            FFT fourier = new FFT(fft_size);
            for(int i = 0; i < num_chunks; i++){
                double[] tmp1 = Arrays.copyOfRange(raw[0].fftx, i*fft_size, (i+1) * fft_size);;
                double[] tmp2 = new double[fft_size];
                Log.i("FFT INPUT: ", array_to_string(tmp1));

                fourier.fft(tmp1, tmp2);

                double mean = 0;
                for(int j = 0; j < fft_size; j++){
                    mean += absolute(tmp1[i], tmp2[i]);
                }
                mean /= (float)fft_size;
                Log.i("mean: ", mean+"");
                ret[i] = mean;
            }

            //return ret;
            return normalize_vec(ret);
        }

        protected void onPostExecute(double[] input){
            glob_array = input;
            Log.i("debug", "Means " + array_to_string(glob_array));

            float similar = Float.MAX_VALUE;
            String name = "";
            for(String k : gestureMap.keySet()){
                float tmp = getMostSimilar(glob_array, gestureMap.get(k).means);
                if(tmp < similar){
                    similar = tmp;
                    name = k;
                }

            }

            if(!isExecuting){
                if(similar < 5.0){
                    detectedGesture.setText("Gesture too similar to one that is allready stored, please try again");
                }else {
                    detectedGesture.setText("Gesture stored");
                    String input_name = enterName.getText().toString();
                    gestureMap.put(input_name, new Knock(input_name, glob_array));
                    enterName.setText("");
                }
            } else {

                Log.i("Similar:", name + "  " + similar);
                detectedGesture.setText("Similar: " + name + "  " + similar);
            }
        }
    }

    private float getMostSimilar(double[] p1, double[] p2){
        float sim = Float.MAX_VALUE;
        List p1_indices = getIndices(p1);
        List p2_indices = getIndices(p2);
        float mean_distance = 0;
        int diff_elem = Math.abs(p1_indices.size() - p2_indices.size());
        Log.i("debug", "diff_num_knocks: " + diff_elem);
        if(diff_elem == 0){
            // if equal number of knocks detected, return isSImilar of unmodified input
            float tmp = isSimilar(p1_indices, p2_indices);
            Log.i("debug ", "same number of knocks, getMostSimilar returns: " + tmp);
            return tmp;
        }else if(diff_elem == 1){

            // maybe 2 knocks got into the same window
            // to recognize those patterns too, we evaluate all possible inplace combinations
            // against each other
            List tmp1 = new ArrayList<>();
            List tmp2;
            // tmp2 is allways the pattern of smaller size, hence only tmp1 needs to be modified
            if(p1_indices.size() > p2_indices.size()){
                tmp2 = p2_indices;
            }else{
                tmp2 = p1_indices;
            }
            for(int i = 0; i < Math.max(p1_indices.size(), p2_indices.size()); i++){
                // "10 +" to amrk that the patterns where not of same length
                sim = Math.min(sim, 10 + isSimilar(get_list_without_i(tmp1, i), tmp2));
            }
            Log.i("debug: ", "get_most_similar returns " + sim);
            return sim;
        }else{
            Log.i("debug ", "difference in knock number too high");
            return Float.MAX_VALUE;
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


    private float isSimilar(List p1_indices, List p2_indices) {
        Log.i("Debug ", "is_similar has been called");
        float mean_distance = 0;
//        if(p1_indices.size() == p2_indices.size()){
        for (int i = 0; i < p1_indices.size() - 1; i++){
            mean_distance += Math.abs(((int) p1_indices.get(i+1) - (int) p1_indices.get(i))
                                - ((int) p2_indices.get(i+1) - (int) p2_indices.get(i)));
            //mean_distance += Math.abs((int) p1_indices.get(i) - (int) p2_indices.get(i));
        }
        if(p1_indices.size() > 1){
            mean_distance /= (float) (p1_indices.size() - 1);
        }
//        }
//        else{
//            return Float.MAX_VALUE;
//        }
        //return Math.abs(mean_distance - Math.abs((int) p1_indices.get(0) - (int) p2_indices.get(0)));
        Log.i("Debug ", "is_similar returns " + mean_distance);
        return mean_distance;
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
