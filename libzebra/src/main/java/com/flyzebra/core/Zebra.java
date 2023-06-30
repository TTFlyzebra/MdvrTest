package com.flyzebra.core;

import com.flyzebra.utils.FlyLog;

public class Zebra {
    static {
        System.loadLibrary("zebra");
    }

    private long pRtmpPointer = -1;


    private static class ZebraNativeHolder {
        public static final Zebra sInstance = new Zebra();
    }

    public static Zebra get() {
        return Zebra.ZebraNativeHolder.sInstance;
    }

    public void init() {
        FlyLog.d("ZebraNative init!");
        pRtmpPointer = _init();
    }

    public void release() {
        if (pRtmpPointer < 0) return;
        long pointer = pRtmpPointer;
        pRtmpPointer = -1;
        _release(pointer);
        FlyLog.d("ZebraNative release!");
    }

    private native long _init();

    private native void _release(long p_obj);

}
