package com.xing.anyadvertisedapp;

import android.app.Application;

import com.xing.android.adtracker.Logger;

public class SampleApplication extends Application {

    public SampleApplication() {
        super();
        Logger.setLoggingEnabled(true);
    }
}
