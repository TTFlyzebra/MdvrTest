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

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraEncoder implements VideoCodecCB, INotify {
    private final int mChannel;
    private int width;
    private int height;
    private final int yuvSize;
    private final int yuvBufSize;
    private ByteBuffer yuvBuf = null;
    private Thread yuvThread;
    private final Object yuvLock = new Object();
    private final AtomicBoolean is_stop = new AtomicBoolean(true);

    public CameraEncoder(int channel, int width, int height) {
        this.mChannel = channel;
        this.width = width;
        this.height = height;
        yuvSize = width * height * 3 / 2;
        yuvBufSize = yuvSize + 8;
        yuvBuf = ByteBuffer.wrap(new byte[yuvBufSize * 2]);
    }

    public void onCreate() {
        FlyLog.d("CameraEncoder[%d] start!", mChannel);
        Notify.get().registerListener(this);
        is_stop.set(false);
        yuvThread = new Thread(() -> {
            long ptsUsec = 0;
            byte[] yuv = new byte[yuvSize];
            VideoCodec videoCodec = new VideoCodec(mChannel, this);
            videoCodec.initCodec(Config.CAM_MIME_TYPE, width, height, Config.CAM_BIT_RATE);
            while (!is_stop.get()) {
                synchronized (yuvLock) {
                    if (!is_stop.get() && yuvBuf.position() < yuvBufSize) {
                        try {
                            yuvLock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (is_stop.get()) break;
                    yuvBuf.flip();
                    ptsUsec = yuvBuf.getLong();
                    yuvBuf.get(yuv);
                    yuvBuf.compact();
                }
                videoCodec.inYuvData(yuv, yuvSize, ptsUsec);
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
        if (NotifyType.NOTI_CAMOUT_YUV == type && yuvBuf != null) {
            int channel = ByteUtil.bytes2Short(params, 0, true);
            if (this.mChannel != channel) return;
            long ptsUsec = ByteUtil.bytes2Long(params, 6, true);
            synchronized (yuvLock) {
                try {
                    if (yuvBuf.remaining() < yuvBufSize) {
                        FlyLog.w("yuv buffer is full, lost one frame %d!", ptsUsec);
                        yuvBuf.clear();
                    }
                    yuvBuf.putLong(ptsUsec);
                    yuvBuf.put(data);
                    yuvLock.notify();
                } catch (Exception e) {
                    FlyLog.e(e.toString());
                }
            }
        }
    }
}
