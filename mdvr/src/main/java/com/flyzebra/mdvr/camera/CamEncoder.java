/**
 * FileName: RtmpPushService
 * Author: FlyZebra
 * Email:flycnzebra@gmail.com
 * Date: 2023/6/23 11:07
 * Description:
 */
package com.flyzebra.mdvr.camera;

import android.os.SystemClock;

import com.flyzebra.core.media.VideoCodec;
import com.flyzebra.core.media.VideoCodecCB;
import com.flyzebra.core.notify.INotify;
import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.NotifyType;
import com.flyzebra.mdvr.Config;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

import java.util.concurrent.atomic.AtomicBoolean;

public class CamEncoder implements VideoCodecCB, INotify {
    private final int mChannel;
    private final int width;
    private final int height;
    private final int frame_rate;
    private final int i_frame_interval;
    private final int bitrate;
    private final int bitrate_mode;
    private byte[] yuvBuf = null;
    private long ptsUsec = 0;
    private Thread yuvThread;
    private final Object yuvLock = new Object();
    private final AtomicBoolean is_stop = new AtomicBoolean(true);

    public CamEncoder(int channel, int width, int height, int frame_rate, int i_frame_interval, int bitrate, int bitrate_mode) {
        this.mChannel = channel;
        this.width = width;
        this.height = height;
        this.frame_rate = frame_rate;
        this.i_frame_interval = i_frame_interval;
        this.bitrate = bitrate;
        this.bitrate_mode = bitrate_mode;
    }

    public void onCreate() {
        FlyLog.d("CameraEncoder[%d] start!", mChannel);
        Notify.get().registerListener(this);
        is_stop.set(false);
        yuvThread = new Thread(() -> {
            VideoCodec videoCodec = new VideoCodec(mChannel, this);
            videoCodec.initCodec(Config.CAM_MIME_TYPE, width, height, frame_rate, i_frame_interval, bitrate, bitrate_mode);
            int size = width * height * 3 / 2;
            byte[] tempData = new byte[size];
            long pts = 0;
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
                    System.arraycopy(yuvBuf, 0, tempData, 0, size);
                    pts = ptsUsec;
                    yuvBuf = null;
                }
                videoCodec.inYuvData(tempData, size, pts);
                //FlyLog.e("encoder one yuv frame use %s millis.", SystemClock.uptimeMillis() - stime);
            }
            videoCodec.releaseCodec();
        }, "cam-avc" + mChannel);
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
        long stime = SystemClock.uptimeMillis();
        byte[] params = new byte[10];
        ByteUtil.shortToBytes((short) channel, params, 0, true);
        ByteUtil.longToBytes(pts, params, 2, true);
        Notify.get().handledata(NotifyType.NOTI_CAMOUT_AVC, data, size, params);
        long utime = SystemClock.uptimeMillis() - stime;
        if (utime > 50) {
            FlyLog.e("notifyAvcData  use time %d, data size %d", utime, size);
        }
    }

    @Override
    public void notifyHevcData(int channel, byte[] data, int size, long pts) {
        byte[] params = new byte[10];
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
