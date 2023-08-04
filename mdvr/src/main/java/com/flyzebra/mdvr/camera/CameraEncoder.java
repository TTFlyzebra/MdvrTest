/**
 * FileName: RtmpPushService
 * Author: FlyZebra
 * Email:flycnzebra@gmail.com
 * Date: 2023/6/23 11:07
 * Description:
 */
package com.flyzebra.mdvr.camera;

import com.flyzebra.core.media.VideoCodec;
import com.flyzebra.core.media.VideoCodecCB;
import com.flyzebra.core.notify.INotify;
import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.NotifyType;
import com.flyzebra.mdvr.Config;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

import java.util.concurrent.atomic.AtomicBoolean;

public class CameraEncoder implements VideoCodecCB, INotify {
    private final int mChannel;
    private int width;
    private int height;
    private byte[] yuvBuf = null;
    private long ptsUsec = 0;
    private Thread yuvThread;
    private final Object yuvLock = new Object();
    private final AtomicBoolean is_stop = new AtomicBoolean(true);

    public CameraEncoder(int channel, int width, int height) {
        this.mChannel = channel;
        this.width = width;
        this.height = height;
    }

    public void onCreate() {
        FlyLog.d("CameraEncoder[%d] start!", mChannel);
        Notify.get().registerListener(this);
        is_stop.set(false);
        yuvThread = new Thread(() -> {
            VideoCodec videoCodec = new VideoCodec(mChannel, this);
            videoCodec.initCodec(Config.CAM_MIME_TYPE, width, height, Config.CAM_BIT_RATE);
            while (!is_stop.get()) {
                //long stime = SystemClock.uptimeMillis();
                synchronized (yuvLock) {
                    if (!is_stop.get() && yuvBuf == null) {
                        try {
                            yuvLock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (is_stop.get()) break;
                    videoCodec.inYuvData(yuvBuf,  width * height * 3 / 2, ptsUsec);
                    yuvBuf = null;
                }
                //FlyLog.e("encoder one yuv frame use %s millis.", SystemClock.uptimeMillis() - stime);
            }
            videoCodec.releaseCodec();
        }, "camera-avc" + mChannel);
        yuvThread.start();
    }

    public void onDistory() {
        is_stop.set(true);
        Notify.get().unregisterListener(this);
        synchronized (yuvLock) {
            yuvLock.notifyAll();
        }
        try {
            yuvThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        FlyLog.d("CameraEncoder[%d] exit!", mChannel);
    }

    @Override
    public void notifySpsPps(int channel, byte[] data, int size) {
        byte[] params = new byte[4];
        ByteUtil.shortToBytes((short) channel, params, 0, true);
        Notify.get().handledata(NotifyType.NOTI_CAMOUT_SPS, data, size, params);
    }

    @Override
    public void notifyVpsSpsPps(int channel, byte[] data, int size) {
        byte[] params = new byte[4];
        ByteUtil.shortToBytes((short) channel, params, 0, true);
        Notify.get().handledata(NotifyType.NOTI_CAMOUT_SPS, data, size, params);
    }

    @Override
    public void notifyAvcData(int channel, byte[] data, int size, long pts) {
        byte[] params = new byte[12];
        ByteUtil.shortToBytes((short) channel, params, 0, true);
        ByteUtil.longToBytes(pts, params, 2, true);
        Notify.get().handledata(NotifyType.NOTI_CAMOUT_AVC, data, size, params);
    }

    @Override
    public void notifyHevcData(int channel, byte[] data, int size, long pts) {
        byte[] params = new byte[12];
        ByteUtil.shortToBytes((short) channel, params, 0, true);
        ByteUtil.longToBytes(pts, params, 2, true);
        Notify.get().handledata(NotifyType.NOTI_CAMOUT_AVC, data, size, params);
    }

    @Override
    public void notify(byte[] data, int size) {

    }

    @Override
    public void handle(int type, byte[] data, int size, byte[] params) {
        if (NotifyType.NOTI_CAMOUT_YUV == type) {
            int channel = ByteUtil.bytes2Short(params, 0, true);
            if (this.mChannel != channel) return;
            synchronized (yuvLock) {
                try {
                    yuvBuf = data;
                    ptsUsec = ByteUtil.bytes2Long(params, 6, true);
                    yuvLock.notify();
                } catch (Exception e) {
                    FlyLog.e(e.toString());
                }
            }
        }
    }
}
