package com.flyzebra.mdvr.rtmp;

import static com.flyzebra.mdvr.Config.MAX_CAM;

import android.content.Context;

import com.flyzebra.mdvr.Config;

import java.util.Enumeration;
import java.util.Hashtable;

public class RtmpService{
    private final Hashtable<Integer, RtmpPusher> pusherMap = new Hashtable<>();

    public RtmpService(Context context) {

    }

    public void onCreate() {
        for (int i = 0; i < MAX_CAM; i++) {
            RtmpPusher rtmpPusher = new RtmpPusher(i);
            rtmpPusher.onCreate(Config.RTMP_URL + "/camera" + (i + 1));
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
