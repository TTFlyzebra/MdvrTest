package com.flyzebra.mdvr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;

import com.flyzebra.utils.FlyLog;

public class MyRecevier extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.hardware.usb.action.USB_STATE")) {
            FlyLog.d("[%d]USB_STATE: " + intent.toString(), SystemClock.uptimeMillis());
            Bundle bundle = intent.getExtras();
            for (String key : bundle.keySet()) {
                FlyLog.d("[%d]USB_STATE: Key=" + key + ", content=" + bundle.get(key), SystemClock.uptimeMillis());
            }
        }
    }
}
