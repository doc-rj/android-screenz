package com.jones.screenz;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Toast;


public class Sensors {
    private static final String TAG = "Sensors";
    private static boolean TESTING = true;
    
    private static final float DEFAULT_PROX_THRESHOLD = 5.0f;
    private static final int DEFAULT_SHAKE_THRESHOLD = 1200;

    private final Context mContext;
    private final SensorManager mSensorManager;
    private final SensorsReceiver mBroadcastReceiver;
    private final IntentFilter mFilter;
    private boolean mRcvrRegistered;
    
    private final Sensor mProxSensor;
    private final Sensor mAccSensor;

    private float mProxDistance = DEFAULT_PROX_THRESHOLD;
    private int mShakeThreshold = DEFAULT_SHAKE_THRESHOLD;
    
    private long mLastAccUpdate = -1;
    private long mLastAccShake = -1;
    private float mLastAcc_x, mLastAcc_y, mLastAcc_z;
    
    private ProxListener mProxListener;
    private ShakeListener mShakeListener;

    public interface ProxListener {
        public void onProx();
    }
    
    public interface ShakeListener {
        public void onShake();
    }
    
    public Sensors(Context context) {
        mContext = context;
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager == null ) {
            Log.e(TAG, "Failed to get sensor manager!");
        }
        
        if (isProxAvail()) {
            mProxSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        } else {
            Log.w(TAG, "No proximity sensor found!");
            mProxSensor = null;
        }

        if (isAccAvail()) {
            mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        } else {
            Log.w(TAG, "No accelerometer found!");
            mAccSensor = null;
        }

        mBroadcastReceiver = new SensorsReceiver();
        mFilter = new IntentFilter();
        mFilter.addAction(Intent.ACTION_SCREEN_ON);
        mFilter.addAction(Intent.ACTION_SCREEN_OFF);
        // register receiver later when needed
    }

    void destroy() {
        unregisterRcvr();
        mSensorManager.unregisterListener(mSensorEventListener);
    }

    public boolean isProxAvail() {
        return mSensorManager != null &&
            !mSensorManager.getSensorList(Sensor.TYPE_PROXIMITY).isEmpty();
    }

    public boolean isAccAvail() {
        return mSensorManager != null &&
            !mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).isEmpty();
    }
    
    public void addProxListener(ProxListener listener, int distance) {
        mProxListener = listener;
        mProxDistance = distance;
        if (mProxSensor != null) {
            mSensorManager.registerListener(mSensorEventListener, mProxSensor,
                                            SensorManager.SENSOR_DELAY_UI);
            registerRcvr();
        }
    }
    
    public void removeProxListener() {
        mProxListener = null;
        if (mProxSensor != null) {
            mSensorManager.unregisterListener(mSensorEventListener, mProxSensor);
        }
        if (getNumListeners() <= 0) {
            unregisterRcvr();
        }
    }
    
    public void addShakeListener(ShakeListener listener, int shakeSens) {
        mShakeListener = listener;
        mShakeThreshold = shakeSensToThresh(shakeSens);
        if (mAccSensor != null) {
            mSensorManager.registerListener(mSensorEventListener, mAccSensor,
                                            SensorManager.SENSOR_DELAY_GAME);
            registerRcvr();
        }
    }
    
    public void removeShakeListener() {
        mShakeListener = null;
        if (mAccSensor != null) {
            mSensorManager.unregisterListener(mSensorEventListener, mAccSensor);    
        }
        if (getNumListeners() <= 0) {
            unregisterRcvr();
        }        
    }
    
    private int getNumListeners() {
        int num = 0;
        if (mProxListener != null) num++;
        if (mShakeListener != null) num++;
        return num;
    }
    
    private void registerRcvr() {
        if (!mRcvrRegistered) {
            mContext.registerReceiver(mBroadcastReceiver, mFilter);
            mRcvrRegistered = true;
        }
    }
    
    private void unregisterRcvr() {
        if (mRcvrRegistered) {
            mContext.unregisterReceiver(mBroadcastReceiver);
            mRcvrRegistered = false;
        }
    }

    SensorEventListener mSensorEventListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // do nothing
        }

        public void onSensorChanged(SensorEvent event) {
            switch(event.sensor.getType()) {
            case Sensor.TYPE_PROXIMITY:
                if (mProxListener != null && proxNear(event.values)) {
                    Log.d(TAG, "PROX SCREENSHOT!");
                    if (TESTING) {
                        Toast.makeText(mContext, "PROX SCREENSHOT!", Toast.LENGTH_SHORT).show();
                    }
                    mProxListener.onProx();
                }
                break;
                
            case Sensor.TYPE_ACCELEROMETER:
                if (mShakeListener != null && accShake(event.values)) {
                    Log.d(TAG, "SHAKE SCREENSHOT!");
                    if (TESTING) {
                        Toast.makeText(mContext, "SHAKE SCREENSHOT!", Toast.LENGTH_SHORT).show();
                    }
                    mShakeListener.onShake();
                }
                break;
            }
        }
    };
    
    private static int shakeSensToThresh(int sens) {
        return DEFAULT_SHAKE_THRESHOLD;
    }
    
    private boolean proxNear(float[] values) {
        return (values[0] < mProxDistance);
    }
    
    private boolean accShake(float[] values) {
        long now = System.currentTimeMillis();
        // only allow one update every 100 ms
        if ((now - mLastAccUpdate) > 100) {
            long t = (now - mLastAccUpdate);
            mLastAccUpdate = now;
 
            float x = values[0];
            float y = values[1];
            float z = values[2];
 
            float speed = Math.abs(x+y+z - mLastAcc_x - mLastAcc_y - mLastAcc_z) / t * 10000;
            mLastAcc_x = x;
            mLastAcc_y = y;
            mLastAcc_z = z;
            
            // only allow one shake every 2 secs
            if (speed > mShakeThreshold && (now - mLastAccShake) > 2000) {
                mLastAccShake = now;
                return true;
            }
        }
        return false;
    }
    
    public class SensorsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null)
                return;

            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                Log.d(TAG, "Screen turned OFF");
                mSensorManager.unregisterListener(mSensorEventListener);
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                Log.d(TAG, "Screen turned ON");
                if (mProxListener != null) {
                    mSensorManager.registerListener(mSensorEventListener, mProxSensor,
                                                    SensorManager.SENSOR_DELAY_UI);
                }
                if (mShakeListener != null) {
                    mSensorManager.registerListener(mSensorEventListener, mAccSensor,
                                                    SensorManager.SENSOR_DELAY_GAME);
                }
            }
        }
    };
}