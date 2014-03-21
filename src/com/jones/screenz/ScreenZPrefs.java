package com.jones.screenz;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.os.*;
import android.preference.Preference;
import android.preference.CheckBoxPreference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.*;
import android.widget.*;
import android.graphics.*;
import android.util.Log;

public class ScreenZPrefs extends PreferenceActivity
                 implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "ScreenZPrefs";

	private Handler mHandler;
	private ScreenshotRunnable mScreenshotRunnable;
	private IScreenZProvider mProvider;
	private int mSsDelay;

	/*
	 * The ImageView used to display taken screenshots.
	 */
	private ImageView imgScreen;

	private ServiceConnection aslServiceConn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected()");
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected()");
			mProvider = IScreenZProvider.Stub.asInterface(service);
		}
	};

    /** Called when the activity is first created. */
    @Override 
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        mScreenshotRunnable = new ScreenshotRunnable();
        
        addPreferencesFromResource(R.xml.prefs);
        
        Preference takeSsPref = (Preference) findPreference("take_ss_pref");
        takeSsPref.setOnPreferenceClickListener(new OnTakeSsPrefListener());
        
        //setContentView(R.layout.main);

        //imgScreen = (ImageView) findViewById(R.id.imgScreen);
        //Button btn = (Button) findViewById(R.id.btnTakeScreenshot); 
        //btn.setOnClickListener(btnTakeScreenshot_onClick);

        // start service so it stays up
        Intent intent = new Intent(this, ScreenZService.class);
        startService(intent);
        
        // bind to service
        bindService(intent, aslServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
    	unbindService(aslServiceConn);
    	super.onDestroy();
    }
    
    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        finish();
    }

    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        mSsDelay = Integer.parseInt(prefs.getString("delay_pref", "10000"));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals("delay_pref")){
            mSsDelay = Integer.parseInt(prefs.getString("delay_pref", "10000"));
        }
    }
    
/*    
    private void getPrefs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean searchTrigOn = prefs.getBoolean("search_trig_pref", false);
        boolean cameraTrigOn = prefs.getBoolean("camera_trig_pref", false);
        boolean proxTrigOn = prefs.getBoolean("prox_trig_pref", false);
        boolean shakeTrigOn = prefs.getBoolean("shake_trig_pref", false);
    }
*/

    private class OnTakeSsPrefListener implements OnPreferenceClickListener {
        public boolean onPreferenceClick(Preference preference) {
            Toast.makeText(getBaseContext(), "clicked!", Toast.LENGTH_SHORT).show();
            mHandler.postDelayed(mScreenshotRunnable, mSsDelay);
            return true;
        }
    };
    
    private class ScreenshotRunnable implements Runnable {
        @Override
        public void run() {
            if (mProvider == null) {
                return;
            }
            try {
                mProvider.takeScreenshot();
            } catch(RemoteException re) {
                // ignore for now
            }
        }
    };
    
/*
    private View.OnClickListener btnTakeScreenshot_onClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			try {
				if (mProvider == null) {
					Toast.makeText(ScreenZPrefs.this, R.string.n_a, Toast.LENGTH_SHORT).show();
                } else if (!mProvider.isAvailable()) {
					Toast.makeText(ScreenZPrefs.this, R.string.native_n_a, Toast.LENGTH_SHORT).show();
                } else {
					String file = mProvider.takeScreenshot();
					if (file != null) {
						Bitmap screen = BitmapFactory.decodeFile(file);
						imgScreen.setImageBitmap(screen);
					}
				}
			} catch (NotFoundException e) {
                Log.e(TAG, "onClickListener: NotFoundException, e: " + e);
				e.printStackTrace();
			} catch (RemoteException e) {
				Log.e(TAG, "onClickListener: RemoteException, e: " + e);
			}
		}
	};
*/
}
