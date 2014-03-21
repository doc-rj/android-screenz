package com.jones.screenz;

import java.io.*;
import java.util.UUID;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Rect;
import android.os.*;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.widget.Toast;

public class ScreenZService extends Service
                 implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "ScreenZService";
    private static boolean TESTING = true;

	// action name for intent used to bind to service
	public static final String BIND = "com.docjones.ScreenshotService.BIND";  

    // TODO: make this configurable
	private static String SCREENSHOT_FOLDER = "/sdcard/screenz/";

    private final Context mContext = this;
    private SharedPreferences mPrefs;
    private Display mDisplay;
    private Sensors mSensors;
    
    private int mProxDistance;
    private int mShakeSens;

    static {
        System.loadLibrary("screenz");
    }
	
    private static native Bitmap screenshot(int width, int height);
    
	/*
	 * An implementation of interface used by clients to take screenshots.
	 */
	private final IScreenZProvider.Stub mBinder = new IScreenZProvider.Stub() {
        @Override
		public String takeScreenshot() {
            return svcScreenshot();
		}

        @Override
        public boolean isAvailable() {
            return true;
        }
    };

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        mDisplay = wm.getDefaultDisplay();

        mSensors = new Sensors(this);
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        
        if (prefs.getBoolean("prox_trig_pref", false)) {
            mProxDistance = Integer.parseInt(prefs.getString("prox_dist_pref", "5"));
            mSensors.addProxListener(mProxListener, mProxDistance);
        }
        if (prefs.getBoolean("shake_trig_pref", false)) {
            mShakeSens = Integer.parseInt(prefs.getString("shake_sens_pref", "5"));
            Toast.makeText(mContext, String.valueOf(mShakeSens), Toast.LENGTH_LONG).show();
            mSensors.addShakeListener(mShakeListener, 0);
        }
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "onBind()");
		return mBinder;
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // TODO:
        mSensors.destroy();
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals("prox_trig_pref")){
            if (prefs.getBoolean("prox_trig_pref", false)) {
                mProxDistance = Integer.parseInt(prefs.getString("prox_dist_pref", "5"));
                mSensors.addProxListener(mProxListener, mProxDistance);
            } else {
                mSensors.removeProxListener();
            }
        } else
        if (key.equals("shake_trig_pref")) {
            if (prefs.getBoolean("shake_trig_pref", false)) {
                mShakeSens = Integer.parseInt(prefs.getString("shake_sens_pref", "5"));
                Toast.makeText(mContext, String.valueOf(mShakeSens), Toast.LENGTH_LONG).show();
                mSensors.addShakeListener(mShakeListener, 0);
            } else {
                mSensors.removeShakeListener();
            }
        }
    }
    
    private Sensors.ProxListener mProxListener = new Sensors.ProxListener() {
        public void onProx() {
            svcScreenshot();    
        }
    };
    
    private Sensors.ShakeListener mShakeListener = new Sensors.ShakeListener() {
        public void onShake() {
            svcScreenshot();
        }
    };
    
    private String svcScreenshot() {
        String file = null;

        try {
            file = doScreenshot(0, 0);
        } catch(Exception e) {
            Log.e(TAG, "takeScreenshot() exception: " + e);
            e.printStackTrace();
        }
        
        if (TESTING) {
            if (file == null) {
                Toast.makeText(mContext, R.string.screenshot_error, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, R.string.screenshot_ok, Toast.LENGTH_SHORT).show();
            }
        }
        return file;
    }

    /*
     * Takes screenshot and returns image file path.
     */
    private String doScreenshot(int width, int height) {
		Log.d(TAG, "takeScreenshot()");
        final Rect frame = new Rect();
        float scale;

        int dw = mDisplay.getWidth();
        int dh = mDisplay.getHeight();
        int rot;

        // TODO: get frame from user crop or app dimensions?
        frame.union(0, 0, dw, dh);

        // constrain frame to the screen size.
        frame.intersect(0, 0, dw, dh);
        if (frame.isEmpty()) {
		    Log.e(TAG, "Empty frame!");
            return null;
        }

        // screenshot API does not apply the current screen rotation
        rot = mDisplay.getRotation();
        int fw = frame.width();

        // first try reducing to fit in x dimension
        scale = width/(float)fw;

        // screenshot will contain the entire screen
        dw = (int)(dw*scale);
        dh = (int)(dh*scale);

        // TODO: investigate landscape display upside down
        if (rot == Surface.ROTATION_90 || rot == Surface.ROTATION_270) {
            int tmp = dw;
            dw = dh;
            dh = tmp;
            rot = (rot == Surface.ROTATION_90) ? Surface.ROTATION_270 : Surface.ROTATION_90;
        }
        
        // fetch the screen and save it
        Bitmap rawss = null;
        try {
            rawss = screenshot(dw, dh);
            if (rawss == null) {
		        Log.e(TAG, "Surface.screenshot() failed!");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Surface.screenshot() exception: " + e); 
            e.printStackTrace();
            return null;
        }

        //Bitmap bm = Bitmap.createBitmap(width, height, rawss.getConfig());      
        //return writeImageFile(bm);
        String path = writeImageFile(rawss);
        if (path == null) {
            Log.e(TAG, "writeImageFile() failed!");
        }

        return path;
    }
	
	/*
	 * Saves bitmap to PNG file.
	 */
	private String writeImageFile(Bitmap bm) {
        // make sure the path to save screens exists
        File screensPath = new File(SCREENSHOT_FOLDER);
        try {
            if (!screensPath.mkdirs()) {
                Log.e(TAG, "mkdirs() failed! path: " + screensPath);
                return null;
            }
        } catch(SecurityException se) {
            Log.e(TAG, "mkdirs() SecurityException: " + se);
            return null;
        }

        // construct screenshot file name
        StringBuilder sb = new StringBuilder();
        sb.append(SCREENSHOT_FOLDER);
        // hash code of UUID should be quite random yet short
        sb.append(Math.abs(UUID.randomUUID().hashCode()));
        sb.append(".png");
        String file = sb.toString();

		// save it in PNG format
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException: " + file);
            return null;
		}
		
		if (!bm.compress(CompressFormat.PNG, 100, fos)) {
            Log.e(TAG, "bm.compress() failed!");
		    return null;
		}
		
		return file;
	}
}
