package com.flyzebra.mdvr.rtmp;

import static com.flyzebra.mdvr.Config.MAX_CAM;

import android.content.Context;
import android.text.TextUtils;

import com.flyzebra.mdvr.Config;
import com.flyzebra.utils.IDUtil;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;

public class RtmpServer {
    private Context mContext;
    private final Hashtable<Integer, RtmpPusher> pusherMap = new Hashtable<>();

    public RtmpServer(Context context) {
        mContext = context;
    }

    public void onCreate() {
        String imei = IDUtil.getIMEI(mContext);
        if(TextUtils.isEmpty(imei)) imei = "860123456789012";
        for (int i = 0; i < MAX_CAM; i++) {
            RtmpPusher rtmpPusher = new RtmpPusher(i);
            rtmpPusher.onCreate(Config.RTMP_URL + File.separator + imei + "/camera" + (i + 1));
            pusherMap.put(i, rtmpPusher);
        }
    }

    public void onDestory() {
        Enumeration<RtmpPusher> elements = pusherMap.elements();
        while (elements.hasMoreElements()) {
            elements.nextElement().onDestory();
        }
    }
}
