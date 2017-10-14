package com.xing.anyadvertisedapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.xing.android.adtracker.XNGAdTrackerReceiver;

/**
 * sample for any other {@link BroadcastReceiver} registered to the same INSTALL_REFERRER action.
 */
public class Receiver extends BroadcastReceiver {
    private static final String TAG = "Receiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive called for action " + intent.getAction());

        /* if you are registering your own BroadcastReceiver for the INSTALL_REFERRER action in the application's manifest
         * instead of the XNGAdTrackerReceiver, you must invoke the XNGAdTrackerReceiver as part of your onReceive() handling
         * like in the following line:
         */
        // XNGAdTrackerReceiver.handleBroadcastIntent(context, intent);
    }
}
