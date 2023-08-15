package com.flyzebra.mdvr.wifi;

import android.content.Context;

public class WifiService {
    private final Context mContext;

    public WifiService(Context context) {
        mContext = context;
    }

    public void onCreate() {
        //Settings.Global.putString(mContext.getContentResolver(), "captive_portal_http_url", "http://connect.rom.miui.com/generate_204");
        //Settings.Global.putString(mContext.getContentResolver(), "captive_portal_https_url", "https://connect.rom.miui.com/generate_204");
        //WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        //if(!wifiManager.isWifiEnabled()){
        //    wifiManager.setWifiEnabled(true);
        //}
        //WifiConfiguration wifiConfig = new WifiConfiguration();
        //wifiConfig.SSID = Config.WIFI_SSID;
        //wifiConfig.preSharedKey = Config.WIFI_PSWD;
        //int netId = wifiManager.addNetwork(wifiConfig);
        //wifiManager.disconnect();
        //wifiManager.enableNetwork(netId, true);
        //wifiManager.reconnect();
    }

    public void onDestory() {
    }

}
