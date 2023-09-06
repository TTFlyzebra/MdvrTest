package com.flyzebra.core;

import android.content.Context;

import com.flyzebra.core.notify.INotify;
import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.Protocol;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;
import com.flyzebra.utils.IDUtil;

public class Fzebra implements INotify {
    static {
        System.loadLibrary("zebra");
    }

    private Context mContext;
    private long _ptr_obj = -1;
    private long mTid = 0;

    private Fzebra() {
    }

    private static class ZebraNativeHolder {
        public static final Fzebra sInstance = new Fzebra();
    }

    public static Fzebra get() {
        return Fzebra.ZebraNativeHolder.sInstance;
    }

    public void init(Context context) {
        mContext = context;
        _ptr_obj = _init();
        try {
            String imei = IDUtil.getIMEI(mContext);
            mTid = Long.parseLong(imei);
            _setTid(mTid);
        } catch (Exception e) {
            FlyLog.e();
        }
        Notify.get().registerListener(this);
    }

    public void release() {
        Notify.get().unregisterListener(this);
        if (_ptr_obj < 0) return;
        long pointer = _ptr_obj;
        _ptr_obj = -1;
        _release(pointer);
    }

    public long getTid() {
        return mTid;
    }

    @Override
    public void notify(byte[] data, int size) {
        if (_ptr_obj < 0) return;
        short type = ByteUtil.bytes2Short(data, 2, true);
        switch (type) {
            case Protocol.TYPE_TU_HEARTBEAT:
            case Protocol.TYPE_SCREEN_T_START:
            case Protocol.TYPE_SCREEN_T_STOP:
            case Protocol.TYPE_SNDOUT_T_START:
            case Protocol.TYPE_SNDOUT_T_STOP:
            case Protocol.TYPE_CAMOUT_T_START:
            case Protocol.TYPE_CAMOUT_T_STOP:
            case Protocol.TYPE_MICOUT_T_START:
            case Protocol.TYPE_MICOUT_T_STOP:
            case Protocol.TYPE_CAMFIX_T_START:
            case Protocol.TYPE_CAMFIX_T_STOP:
            case Protocol.TYPE_MICFIX_T_START:
            case Protocol.TYPE_MICFIX_T_STOP:
            case Protocol.TYPE_CAMERA_OPEN:
            case Protocol.TYPE_CAMERA_CLOSE:
            case Protocol.TYPE_INPUT_MULTI_S_READY:
            case Protocol.TYPE_INPUT_MULTI_S_STOP:
                _notify(_ptr_obj, data, size);
                break;
        }
    }

    @Override
    public void handle(int type, byte[] data, int size, byte[] params) {
        if (_ptr_obj < 0) return;
        _handle(_ptr_obj, type, data, size, params);
    }

    public void startUserServer() {
        if (_ptr_obj < 0) return;
        _startUserServer(_ptr_obj);
    }

    public void stopUserServer() {
        if (_ptr_obj < 0) return;
        _stopUserServer(_ptr_obj);
    }

    public void startRtspServer() {
        if (_ptr_obj < 0) return;
        _startRtspServer(_ptr_obj);
    }

    public void stopRtspServer() {
        if (_ptr_obj < 0) return;
        _stopRtspServer(_ptr_obj);
    }

    private void javaNotifydata(byte[] data, int size) {
        Notify.get().notifydata(data, size);
    }

    private void javaHandleData(int type, byte[] data, int size, byte[] params) {
        //Notify.get().handledata(type, data, size, params);
    }

    private native long _init();

    private native void _release(long p_obj);

    private native void _setTid(long tid);

    private native void _notify(long p_obj, byte[] data, int size);

    private native void _handle(long p_obj, int type, byte[] data, int size, byte[] params);

    private native void _startUserServer(long p_obj);

    private native void _stopUserServer(long p_obj);

    private native void _startRtspServer(long p_obj);

    private native void _stopRtspServer(long p_obj);

}
