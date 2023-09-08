package com.flyzebra.mdvr.wifip2p;

import static android.os.Looper.getMainLooper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;

import com.flyzebra.core.Fzebra;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

public class WifiP2PService {
    private Context mContext;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel wifChannel;
    private IntentFilter intentFilter;
    private MyRecevier myRecevier;
    private final AtomicBoolean is_stop = new AtomicBoolean(true);

    public WifiP2PService(Context context) {
        mContext = context;
    }

    public void start() {
        FlyLog.d("WifiP2PService start!");
        is_stop.set(false);
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        wifiP2pManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
        wifChannel = wifiP2pManager.initialize(mContext, getMainLooper(), null);

        try {
            Class<?> clz = wifiP2pManager.getClass();
            Method method = clz.getMethod("setDeviceName", Channel.class, String.class, ActionListener.class);
            method.setAccessible(true);
            String name = "MD201_" + ByteUtil.longToSysId(Fzebra.get().getTid());
            method.invoke(wifiP2pManager, wifChannel, name, null);
        } catch (Exception e) {
            FlyLog.e(e.toString());
        }

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        myRecevier = new MyRecevier();
        mContext.registerReceiver(myRecevier, intentFilter);

        //wifiP2pManager.createGroup(wifChannel, new ActionListener() {
        //    @Override
        //    public void onSuccess() {
        //        FlyLog.d("wifi createGroup success");
        //    }
        //    @Override
        //    public void onFailure(int i) {
        //        FlyLog.e("wifi createGroup failure %d", i);
        //    }
        //});
    }

    public void stop() {
        is_stop.set(true);
        mContext.unregisterReceiver(myRecevier);
        //wifiP2pManager.removeGroup(wifChannel, new ActionListener() {
        //    @Override
        //    public void onSuccess() {
        //        FlyLog.d("wifi removeGroup success");
        //    }
        //    @Override
        //    public void onFailure(int i) {
        //        FlyLog.e("wifi removeGroup failure %d", i);
        //    }
        //});
        FlyLog.d("WifiP2PService stop!");
    }

    private class MyRecevier extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            }
        }
    }
}
