package com.flyzebra.mdvr.rtmp;

import android.content.Context;

import com.flyzebra.core.notify.INotify;
import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.NotifyType;
import com.flyzebra.mdvr.Config;
import com.flyzebra.utils.ByteUtil;

import java.util.Enumeration;
import java.util.Hashtable;

public class RtmpService implements INotify {
    private Hashtable<Integer, byte[]> videoHeadMap = new Hashtable<>();
    private Hashtable<Integer, byte[]> audioHeadMap = new Hashtable<>();
    private final Hashtable<Integer, RtmpPusher> pusherMap = new Hashtable<>();

    public RtmpService(Context context) {

    }

    public void onCreate() {
        Notify.get().registerListener(this);
        for (int i = 0; i < 4; i++) {
            RtmpPusher rtmpPusher = new RtmpPusher(i);
            rtmpPusher.start(Config.RTMP_URL + "/camera" + i);
            pusherMap.put(i, rtmpPusher);
        }
    }

    public void onDestory() {
        Notify.get().unregisterListener(this);
        Enumeration<RtmpPusher> elements = pusherMap.elements();
        while (elements.hasMoreElements()) {
            elements.nextElement().stop();
        }
    }

    @Override
    public void notify(byte[] data, int size) {

    }

    @Override
    public void handle(int type, byte[] data, int size, byte[] params) {
        if (NotifyType.NOTI_MICOUT_SPS == type) {
            short channel = ByteUtil.bytes2Short(params, 0, true);
            byte[] audioHead = new byte[size];
            System.arraycopy(data, 0, audioHead, 0, size);
            audioHeadMap.put((int) channel, audioHead);
        } else if (NotifyType.NOTI_CAMOUT_SPS == type) {
            short channel = ByteUtil.bytes2Short(params, 0, true);
            byte[] videoHead = new byte[size];
            System.arraycopy(data, 0, videoHead, 0, size);
            videoHeadMap.put((int) channel, videoHead);
        }
    }
}
