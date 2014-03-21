package com.jones.screenz;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;


public class ScreenZBgActivity extends Activity {
    private static final String TAG = "ScreenZBgActivity";

    private ServiceConnection aslServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected()");
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected()");
            IScreenZProvider provider = IScreenZProvider.Stub.asInterface(service);
            try {
                provider.takeScreenshot();
            } catch(RemoteException re) {
                // ignore for now
            }
            finish();
        }
    };
    
    /** Called when the activity is first created. */
    @Override 
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, ScreenZService.class);
        startService(intent);
        bindService(intent, aslServiceConn, Context.BIND_AUTO_CREATE);
    }
}