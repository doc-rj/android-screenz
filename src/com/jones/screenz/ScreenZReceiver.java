package com.jones.screenz;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receives and handles system broadcasts
 */
public class ScreenZReceiver extends BroadcastReceiver {
    private static final String TAG = "ScreenZReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }
    }
}