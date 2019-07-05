package com.example.android.bluetoothchat;

import android.content.Context;
import android.renderscript.Matrix3f;
import android.renderscript.RenderScript;

import com.example.android.iirj.Butterworth;

public class PositionHelper {



    private float[] veccPrev = new float[]{0f,0f,0f};
    private float[] posPrev = new float[]{0f,0f,0f};
    private float time = 1f/256f;

    private FilterButterworth filterButterwothVx = new FilterButterworth();
    private FilterButterworth filterButterwothVy = new FilterButterworth();
    private FilterButterworth filterButterwothVz = new FilterButterworth();

    private FilterButterworth filterButterwothPx = new FilterButterworth();
    private FilterButterworth filterButterwothPy = new FilterButterworth();
    private FilterButterworth filterButterwothPz = new FilterButterworth();


    public PositionHelper(){
        filterButterwothVx.setFilter(1.0f, 256, FilterButterworth.Highpass);
        filterButterwothVy.setFilter(1.0f, 256, FilterButterworth.Highpass);
        filterButterwothVz.setFilter(1.0f, 256, FilterButterworth.Highpass);

        filterButterwothPx.setFilter(1.0f, 256, FilterButterworth.Highpass);
        filterButterwothPy.setFilter(1.0f, 256, FilterButterworth.Highpass);
        filterButterwothPz.setFilter(1.0f, 256, FilterButterworth.Highpass);

    }

    public float[] calculatePosition(float[] acc, float[] deltaRotationMatrix){

        float[] left = new float[9];
        left[0] = 1.0f;
        left[1] = 0f;
        left[2] = 0f;
        left[3] = 0f;
        left[4] = 1f;
        left[5] = 0f;
        left[6] = 0f;
        left[7] = 0f;
        left[8] = 1f;

        if(deltaRotationMatrix!=null) left = deltaRotationMatrix.clone();

        float[] tcAcc = multiply(left,acc);

        float[] pos = new float[3];
        float[] posF = new float[3];

        float vec0 = veccPrev[0] + tcAcc[0]*time;
        float vec0F =  filterButterwothVx.filter(vec0);
        veccPrev[0] = vec0;
        pos[0] = posPrev[0] + vec0F*time;
        posF[0] =  filterButterwothPx.filter(pos[0]);
        posPrev[0] = pos[0];


        float vec1 = veccPrev[1] + tcAcc[1]*time;
        float vec1F =  filterButterwothVy.filter(vec1);
        veccPrev[1] = vec1;
        pos[1] = posPrev[1] + vec1F*time;
        posF[1] =  filterButterwothPy.filter(pos[1]);
        posPrev[1] = pos[1];

        float vec2 = veccPrev[2] + tcAcc[2]*time;
        float vec2F =  filterButterwothVz.filter(vec2);
        veccPrev[2] = vec2;
        pos[2] = posPrev[2] + vec2F*time;
        posF[2] =  filterButterwothPz.filter(pos[2]);
        posPrev[2] = pos[2];

        return posF;
    }

    private float[] multiply(float[] left, float[] right){
        float[] result = new float[3];
        result[0] = left[0]*right[0]+left[1]*right[1]+left[2]*right[2];
        result[1] = left[3]*right[0]+left[4]*right[1]+left[5]*right[2];
        result[2] = left[6]*right[0]+left[7]*right[1]+left[8]*right[2];
        return result;
    }

    public float filter(float newInput, float inputPrev)
    {
        return 0.5f*newInput - 0.5f*inputPrev;
    }


}
