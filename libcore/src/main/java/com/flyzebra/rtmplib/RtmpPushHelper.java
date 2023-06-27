package com.flyzebra.rtmplib;

import com.flyzebra.utils.FlyLog;

public class RtmpPushHelper {

    static {
        System.loadLibrary("rtmp");
    }

    public RtmpPushHelper(){
        FlyLog.d("RtmpPushHelper()");
    }

    private native long openRtmpPushUrl(String url);

    private native int pushSpsPps(long id, byte[] sps, int spsLen, byte[] pps, int ppsLen);

    private native int pushSpsPpsVps(long id, byte[] sps, int spsLen, byte[] pps, int ppsLen, byte[] vps, int vpsLen);

    private native int pushVideoFrame(long id, byte[] data, int dataLen, boolean keyFrame, boolean isH264);

    private native int pushAudioHeader(long id, byte[] data, int dataLen);

    private native int pushAudioFrame(long id, byte[] data, int dataLen);

    private native void closeRtmpPush(long id);

}