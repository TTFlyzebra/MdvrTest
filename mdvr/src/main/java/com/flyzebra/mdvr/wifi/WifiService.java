package com.flyzebra.mdvr.wifi;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.text.format.Formatter;

import com.flyzebra.mdvr.Config;
import com.flyzebra.utils.FlyLog;

public class WifiService {
    private final Context mContext;

    public WifiService(Context context) {
        mContext = context;
    }

    public void start() {
        Settings.Global.putString(mContext.getContentResolver(), "captive_portal_http_url", "http://connect.rom.miui.com/generate_204");
        Settings.Global.putString(mContext.getContentResolver(), "captive_portal_https_url", "https://connect.rom.miui.com/generate_204");
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if(!wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(true);
        }

        boolean is_wifi_connected = false;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if(wifiInfo.getSupplicantState()== SupplicantState.COMPLETED){
            String ssid = wifiInfo.getSSID();
            String ipAddress = Formatter.formatIpAddress(wifiInfo.getIpAddress());
            String macAddress = wifiInfo.getMacAddress();
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            String gateway = Formatter.formatIpAddress(dhcpInfo.gateway);
            FlyLog.d("Connect wifi ssid:%s, ip:%s, gateway:%s,  mac:%s.", ssid, ipAddress, gateway, macAddress);
            is_wifi_connected = Config.WIFI_SSID.equals(ssid);
        }

        if(!is_wifi_connected) {
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = Config.WIFI_SSID;
            wifiConfig.preSharedKey = Config.WIFI_PSWD;
            int netId = wifiManager.addNetwork(wifiConfig);
            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            wifiManager.reconnect();
        }
    }

    public void stop() {
    }

}
