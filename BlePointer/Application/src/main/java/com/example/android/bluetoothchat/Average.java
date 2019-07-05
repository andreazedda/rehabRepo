package com.example.android.bluetoothchat;

import java.util.ArrayList;
import java.util.List;

public class Average {

    public static int LENGTH = 7;
    public static float offset = 0.01f;

    private float[] window;
    public int counter;
    private int index;
    private float max = 0.5f;
    private float min = -0.5f;
    private float rmax = 1f;
    private float rmin = -1f;
    private int expo = 5;


    public Average(){

        window = new float[LENGTH];
        for(int i = 0; i< LENGTH; i++) window[i] = 0f;
        counter = 0;
        index = 0;

    }

    public int getCounter(){
        return counter;
    }

    public void addSamples(float sample){
        window[index % LENGTH] = sample;
        index++;
        if(counter < LENGTH) counter ++;
    }

    public float normalize (float sample){
        if (sample > max || sample < min ) return sample;
        // normalizzazione [0,1]
        float range = max - min;
        float e_sample = (sample-min)/range;
        // scale to [-1 , 1]
        float range2 = rmax - rmin;
        float normalize = (e_sample * range2) + rmin;
        return (float) (Math.abs(sample) * (Math.pow(normalize,expo)));
    }


    public float average(){
        if(counter == 0) return 0;

        float sum = 0f;
        for(int i = 0; i < counter; i++){
            sum += window[i];
        }
        return  sum/counter;
    }

}
