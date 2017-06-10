package com.example.linux.knockgesture;

import android.util.Log;

/**
 * Created by linux on 03.06.17.
 */
public class Knock {
    public String name;
    double[] means;

    public Knock(String n, double[] m){
        name = n;
        means = new double[m.length];
        for(int i = 0; i < m.length; i++){
            means[i] = m[i];
        }
    }

    public void print(){
        Log.i(name, array_to_string(means));
    }

    private String array_to_string(double[] doubles){
        String out = "";
        for(double d : doubles){
            out += d + ", ";
        }
        return out;
    }
}
