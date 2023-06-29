package com.flyzebra.rtmp;

import com.flyzebra.utils.FlyLog;

/**
 * Created by lake on 16-3-30.
 */
public class RtmpDump {
    static {
        System.loadLibrary("rtmpdump");
    }

    private long pRtmpPointer = -1;
    private int mChannel;

    public RtmpDump() {
    }

    public boolean open(int channel, String rtmp_url) {
        this.mChannel = channel;
        pRtmpPointer = _open(mChannel, rtmp_url);
        if(pRtmpPointer > 0){
            FlyLog.d("RtmpDump open [%d][%s] success!", mChannel, rtmp_url);
            return true;
        }else{
            FlyLog.e("RtmpDump open [%d][%s] fail!", mChannel, rtmp_url);
            return false;
        }
    }

    public void close() {
        if (pRtmpPointer < 0) return;
        long pointer = pRtmpPointer;
        pRtmpPointer = -1;
        _close(pointer);
        FlyLog.d("RtmpDump[%d] release!", mChannel);
    }

    public boolean writeAacHead(byte[] head, int size) {
        if (pRtmpPointer < 0) return false;
        return _writeAacHead(pRtmpPointer, head, size);
    }

    public boolean writeAacData(byte[] data, int size, long pts) {
        if (pRtmpPointer < 0) return false;
        return _writeAacData(pRtmpPointer, data, size, pts);
    }

    public boolean writeAvcHead(byte[] data, int size) {
        if (pRtmpPointer < 0) return false;
        return _writeAvcHead(pRtmpPointer, data, size);
    }

    public boolean writeAvcData(byte[] data, int size, long pts) {
        if (pRtmpPointer < 0) return false;
        return _writeAvcData(pRtmpPointer, data, size, pts);
    }

    public boolean writeHevcHead(byte[] data, int size) {
        if (pRtmpPointer < 0) return false;
        return _writeHevcHead(pRtmpPointer, data, size);
    }

    public boolean writeHevcData(byte[] data, int size, long pts) {
        if (pRtmpPointer < 0) return false;
        return _writeHevcData(pRtmpPointer, data, size, pts);
    }

    public void onError(int errCode) {
        FlyLog.e("RtmpDump[%d] onError %d", mChannel, errCode);
    }

    private native long _open(int channel, String url);

    private native void _close(long pRtmpPointer);

    private native boolean _writeAacHead(long pRtmpPointer, byte[] head, int headLen);

    private native boolean _writeAacData(long pRtmpPointer, byte[] data, int size, long pts);

    private native boolean _writeAvcHead(long pRtmpPointer, byte[] data, int size);

    private native boolean _writeAvcData(long pRtmpPointer, byte[] data, int size, long pts);

    private native boolean _writeHevcHead(long pRtmpPointer, byte[] data, int size);

    private native boolean _writeHevcData(long pRtmpPointer, byte[] data, int size, long pts);

}
