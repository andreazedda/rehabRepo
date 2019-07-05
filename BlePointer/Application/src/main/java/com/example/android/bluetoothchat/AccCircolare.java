package com.example.android.bluetoothchat;

import static java.lang.Math.sqrt;

public class AccCircolare {

    public static float dTime = 0.1f;
    public static float r = 0.4f;

    private float prev;
    private float now;

    public AccCircolare(){
        prev=0.0f;
        now=0.0f;
    }

    public void addSamples(float sample){
        prev = now;
        now = sample;
    }

    public float getAcc(){
        float aTot=0.0f;
        // calcolo accelerazione
        float at = ((now-prev)/dTime) * r;
        float ac = (now * now) * r;
        aTot = (float) Math.sqrt((at*at)+(ac*ac));
        return aTot;

    }

}
