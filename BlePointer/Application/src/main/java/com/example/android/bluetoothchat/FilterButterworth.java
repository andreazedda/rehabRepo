package com.example.android.bluetoothchat;

public class FilterButterworth
{
    /// <summary>
    /// rez amount, from sqrt(2) to ~ 0.1
    /// </summary>
    private  float resonance;

    private  float frequency;
    private  int sampleRate;
    private  int passType;

    public static final int Highpass = 0;
    public static final int Lowpass = 1;

    private  float c, a1, a2, a3, b1, b2;

    /// <summary>
    /// Array of input values, latest are in front
    /// </summary>
    private float[] inputHistory = new float[2];

    /// <summary>
    /// Array of output values, latest are in front
    /// </summary>
    private float[] outputHistory = new float[3];

    public void setFilter(float frequency, int sampleRate, int passType)
    {
        this.resonance = (float) Math.sqrt(2.0d);
        this.frequency = frequency;
        this.sampleRate = sampleRate;
        this.passType = passType;

        switch (passType)
        {
            case Lowpass:
                c = 1.0f / (float)Math.tan(Math.PI * frequency / sampleRate);
                a1 = 1.0f / (1.0f + resonance * c + c * c);
                a2 = 2f * a1;
                a3 = a1;
                b1 = 2.0f * (1.0f - c * c) * a1;
                b2 = (1.0f - resonance * c + c * c) * a1;
                break;
            case Highpass:
                c = (float)Math.tan(Math.PI * frequency / sampleRate);
                a1 = 1.0f / (1.0f + resonance * c + c * c);
                a2 = -2f * a1;
                a3 = a1;
                b1 = 2.0f * (c * c - 1.0f) * a1;
                b2 = (1.0f - resonance * c + c * c) * a1;
                break;
        }
    }


    public float filter(float newInput)
    {
        float newOutput = a1 * newInput + a2 * this.inputHistory[0] + a3 * this.inputHistory[1] - b1 * this.outputHistory[0] - b2 * this.outputHistory[1];

        this.inputHistory[1] = this.inputHistory[0];
        this.inputHistory[0] = newInput;

        this.outputHistory[2] = this.outputHistory[1];
        this.outputHistory[1] = this.outputHistory[0];
        this.outputHistory[0] = newOutput;

        return this.outputHistory[0];
    }

}