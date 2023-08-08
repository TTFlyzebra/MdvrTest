package com.flyzebra.mdvr.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.util.List;
import java.util.Objects;

public class WifiService {
    private Context mContext;
    private WifiManager wifiManager;
    public WifiService(Context context){
        mContext = context;
        wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
    }

    public void onCreate(){
    }

    public void onDestory(){

    }

    public static void enableWifi(Context context, BroadcastReceiver wifiReceiver) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (!Objects.requireNonNull(wifiManager).isWifiEnabled()) {
            //WiFi未打开，启用wifi
            wifiManager.setWifiEnabled(true);
        } else {
            //WiFi已打开，检查是否已连接了，如果已连接，先将连接断开。由于WiFi已经打开，可以直接进行WiFi扫描
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                wifiManager.disableNetwork(wifiInfo.getNetworkId());
                wifiManager.disconnect();
            }
            wifiManager.startScan();
        }
        //注册WiFi扫描结果、WiFi状态变化的广播接收
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        context.registerReceiver(wifiReceiver, intentFilter);
    }

    class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Objects.requireNonNull(action).equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                //扫描结果广播，查找扫描列表是否存在指定SSID的WiFi，如果存在则进行连接
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                List<ScanResult> scanResultList = Objects.requireNonNull(wifiManager).getScanResults();
                for(ScanResult scanResult : scanResultList) {
                    if(scanResult.SSID.equals(ssid)) {
                        connectWifi(ssid, password, wifiCipherType, this)
                    }
                }
            } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                //WiFi状态变化广播：如果WiFi已经完成开启，即可进行WiFi扫描
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_ENABLED:
                        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                        Objects.requireNonNull(wifiManager).startScan();
                        break;
                    case WifiManager.WIFI_STATE_DISABLED:
                        break;
                    default:break;
                }
            }
        }
    }
}
