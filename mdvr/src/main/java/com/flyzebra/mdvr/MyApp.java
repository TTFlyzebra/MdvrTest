package com.flyzebra.mdvr;

import android.app.Application;

import com.flyzebra.utils.FlyLog;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FlyLog.d("##############MDVR Version 1.0.0##############");
    }
}
