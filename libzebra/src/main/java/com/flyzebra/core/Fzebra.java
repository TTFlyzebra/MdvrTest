package com.flyzebra.core;

import com.flyzebra.core.notify.INotify;
import com.flyzebra.core.notify.Notify;
import com.flyzebra.utils.FlyLog;

public class Fzebra implements INotify {
    static {
        System.loadLibrary("zebra");
    }

    private long _ptr_obj = -1;

    private Fzebra(){
    }

    private static class ZebraNativeHolder {
        public static final Fzebra sInstance = new Fzebra();
    }

    public static Fzebra get() {
        return Fzebra.ZebraNativeHolder.sInstance;
    }

    public void init() {
        Notify.get().registerListener(this);
        _ptr_obj = _init();
    }

    public void release() {
        Notify.get().unregisterListener(this);
        if (_ptr_obj < 0) return;
        long pointer = _ptr_obj;
        _ptr_obj = -1;
        _release(pointer);
    }

    @Override
    public void notify(byte[] data, int size) {
        if (_ptr_obj < 0) return;
        _notify(_ptr_obj, data, size);
    }

    @Override
    public void handle(int type, byte[] data, int size, byte[] params) {
        if (_ptr_obj < 0) return;
        _handle(_ptr_obj, type, data, size, params);
    }

    private void javaNotifydata(byte[] data, int size) {
        FlyLog.e("javaNotifydata xxxxxxx");
    }

    private void javaHandleData(int type, byte[] data, int size, byte[] params) {
        FlyLog.e("javaHandleData xxxxxxx");
    }

    private native long _init();

    private native void _release(long p_obj);

    private native void _notify(long p_obj, byte[] data, int size);

    private native void _handle(long p_obj, int type,  byte[] data, int size, byte[] params);

}
