/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothchat;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.example.android.common.logger.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

import Jama.Matrix;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothChatFragment extends Fragment implements SensorEventListener{

    private PrintWriter writer_SENSORS;
    private boolean startStop=false;

    private static final String TAG = "BluetoothChatFragment";
    public static final float EPSILON = 0.000000001f;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView mConversationView;
    private Button mResetButton;
    private Button mStartStopButton;
    private SeekBar mSeekBar;

    //DS inserisco la parte relativa ai sensori
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Sensor mSensorAcceleration;
    private Sensor mSensorGyro;
    private Sensor mSensorMagneticField;
    // DS istanza classe Average
    private float[] deltaRotationMatrix;
    private float[] stampaValori = new float[]{0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f,0f};
    private int contatore = 0;


    private AccCircolare mAccCircolareX;
    private AccCircolare mAccCircolareY;
    private AccCircolare mAccCircolareZ;

    private Average mAveregeX;
    private Average mAveregeY;
    private Average mAveregeZ;

    private Average mFinaleAveregeX;
    private Average mFinaleAveregeY;
    private Average mFinaleAveregeZ;

    private KalmanFilter KF;

    private Date time;
    final float alpha = 0.8f;


    private float tempo = 0.1f;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;

    //DS
    // Create a constant to convert nanoseconds to seconds.
    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private float timestamp;
    private float[] gravity = new float[]{0f, 0f,0f};
    private float[] linear_acceleration = new float[3];


    private PositionHelper mPositionHelper;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        //Requesting permission to read an eternal part of the memory
        //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //DS istanzio le classi per gestire i sensori
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);//DS il metodo getSistemService non posso usarlo in Fragment
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mSensorAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mAccCircolareX = new AccCircolare();
        mAccCircolareY = new AccCircolare();
        mAccCircolareZ = new AccCircolare();

        mAveregeX = new Average();
        mAveregeY = new Average();
        mAveregeZ = new Average();


        mFinaleAveregeX = new Average();
        mFinaleAveregeY = new Average();
        mFinaleAveregeZ = new Average();


        mPositionHelper = new PositionHelper();

        //init filter
        //process parameters
        double dt = 0.1d;
        double processNoiseStdev = 3;
        double measurementNoiseStdev = 5000;
        double m = 0;

        KF = KalmanFilter.buildKF(0, 0, dt, Math.pow(processNoiseStdev, 2) / 2, Math.pow(measurementNoiseStdev, 2));


        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //DS definisco Listener del sensore specificando la velocità di acquisizione dati
        mSensorManager.registerListener(this, mSensor, 3906); //100000 10hz   6666 150hz   3906 256hz
        mSensorManager.registerListener(this, mSensorAcceleration,3906);
        mSensorManager.registerListener(this, mSensorGyro, 100000);
        mSensorManager.registerListener(this, mSensorMagneticField, 3906);
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mConversationView = (ListView) view.findViewById(R.id.in);
        mResetButton = (Button) view.findViewById(R.id.btnReset);
        mStartStopButton = (Button) view.findViewById(R.id.btnStartStop);
        mSeekBar = (SeekBar) view.findViewById(R.id.seekBar);
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);

        mConversationView.setAdapter(mConversationArrayAdapter);


        // Initialize the send button with a listener that for click events
        mResetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                if (null != view) {
                    String message = "Set,reset,1";
                    sendMessage(message);
                }
            }
        });

        mStartStopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                if (null != view) {
                    if(mStartStopButton.getText().equals(getString(R.string.start))) {
                        String message = "Set,start,1";
                        mStartStopButton.setText(R.string.stop);
                        sendMessage(message);
                        startTracking();
                    } else {
                        String message = "Set,stop,1";
                        mStartStopButton.setText(R.string.start);
                        sendMessage(message);
                        stopTraking();
                    }
                }
            }
        });



        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getActivity(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sendMessage("Set,hold,"+i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            message = "$" + message + "$#";
            byte[] send = message.getBytes();
            mChatService.write(send);

        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(startStop) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

                stampaValori[0] = (float) (sensorEvent.values[0]);
                stampaValori[1] = (float) (sensorEvent.values[1]);
                stampaValori[2] = (float) (sensorEvent.values[2]);

                // This timestep's delta rotation to be multiplied by the current rotation
                // after computing it from the gyro sample data.
                if (timestamp != 0) {
                    final float dT = (sensorEvent.timestamp - timestamp) * NS2S;
                    // Axis of the rotation sample, not normalized yet.
                    float axisX = sensorEvent.values[0];
                    float axisY = sensorEvent.values[1];
                    float axisZ = sensorEvent.values[2];

                    // Calculate the angular speed of the sample
                    float omegaMagnitude = (float) sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

                    // Normalize the rotation vector if it's big enough to get the axis
                    // (that is, EPSILON should represent your maximum allowable margin of error)
                    if (omegaMagnitude > EPSILON) {
                        axisX /= omegaMagnitude;
                        axisY /= omegaMagnitude;
                        axisZ /= omegaMagnitude;
                    }

                    // Integrate around this axis with the angular speed by the timestep
                    // in order to get a delta rotation from this sample over the timestep
                    // We will convert this axis-angle representation of the delta rotation
                    // into a quaternion before turning it into the rotation matrix.
                    float thetaOverTwo = omegaMagnitude * dT / 2.0f;
                    float sinThetaOverTwo = (float) sin(thetaOverTwo);
                    float cosThetaOverTwo = (float) cos(thetaOverTwo);
                    deltaRotationVector[0] = sinThetaOverTwo * axisX;
                    deltaRotationVector[1] = sinThetaOverTwo * axisY;
                    deltaRotationVector[2] = sinThetaOverTwo * axisZ;
                    deltaRotationVector[3] = cosThetaOverTwo;
                }
                timestamp = sensorEvent.timestamp;
                deltaRotationMatrix = new float[9];
                SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
                // User code should concatenate the delta rotation we computed with the current rotation
                // in order to get the updated rotation.
                // rotationCurrent = rotationCurrent * deltaRotationMatrix;
                sendMessage("Gyro,quat," +
                        Float.toString(deltaRotationVector[0]) +
                        "," +
                        Float.toString(deltaRotationVector[1]) +
                        "," +
                        Float.toString(deltaRotationVector[2]) +
                        "," +
                        Float.toString(deltaRotationVector[3])
                );

            }

            if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                time = new Date();

                stampaValori[3] = sensorEvent.values[0];
                stampaValori[4] = sensorEvent.values[1];
                stampaValori[5] = sensorEvent.values[2];

                float[] pos = mPositionHelper.calculatePosition(sensorEvent.values, deltaRotationMatrix);

                stampaValori[6] = pos[0];
                stampaValori[7] = pos[1];
                stampaValori[8] = pos[2];


                if (contatore % 25 == 0) { // RICAMPIONA A 10 FOTOGRAMMI PER SECONDO
                    sendMessage("Accelerometer,values," +
                            Float.toString(pos[0]) +
                            "," +
                            Float.toString(pos[1]) +
                            "," +
                            Float.toString(pos[2]) +
                            "," +
                            Long.toString(time.getTime())
                    );
                }

                contatore++;

            }

            writer_SENSORS.print(
                            Float.toString(stampaValori[0]) +
                            "," +
                            Float.toString(stampaValori[1]) +
                            "," +
                            Float.toString(stampaValori[2]) +
                            "," +
                            Float.toString(stampaValori[3]) +
                            "," +
                            Float.toString(stampaValori[4]) +
                            "," +
                            Float.toString(stampaValori[5]) +
                            "," +
                            Float.toString(stampaValori[6]) +
                            "," +
                            Float.toString(stampaValori[7]) +
                            "," +
                            Float.toString(stampaValori[8]) +
                            "\r\n");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    private void startTracking(){
        startStop=true;
        GregorianCalendar datetime = new GregorianCalendar();
        File file = new File(MainActivity.path, "FILE_SENSORS" +
                datetime.get(Calendar.YEAR) + "-" +
                (datetime.get(Calendar.MONTH)+1) + "-" +
                datetime.get(Calendar.DAY_OF_MONTH) + "-" +
                datetime.get(Calendar.HOUR_OF_DAY) + "-" +
                datetime.get(Calendar.MINUTE) + "-" +
                datetime.get(Calendar.SECOND) + ".txt");
        try {
            writer_SENSORS = new PrintWriter(file, "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    private void stopTraking(){
        startStop=false;
        if(writer_SENSORS!=null) writer_SENSORS.close();
    }


}
