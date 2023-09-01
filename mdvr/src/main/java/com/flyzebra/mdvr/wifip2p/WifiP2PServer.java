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

import com.flyzebra.utils.FlyLog;
import com.flyzebra.utils.IDUtil;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

public class WifiP2PServer {
    private Context mContext;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel wifChannel;
    private IntentFilter intentFilter;
    private MyRecevier myRecevier;
    private final AtomicBoolean is_stop = new AtomicBoolean(true);
    private WifiP2PSocket netServer;

    public WifiP2PServer(Context context) {
        mContext = context;
        netServer = new WifiP2PSocket(context);
    }

    public void start() {
        FlyLog.d("WifiP2PServer start!");
        is_stop.set(false);
        netServer.start();
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
            String name = "MD201_" + IDUtil.getIMEI(mContext);
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

        wifiP2pManager.createGroup(wifChannel, null);
    }

    public void stop() {
        is_stop.set(true);
        netServer.stop();
        mContext.unregisterReceiver(myRecevier);
        wifiP2pManager.removeGroup(wifChannel, null);
        netServer.stop();
        FlyLog.d("WifiP2PServer stop!");
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
