/**
 * FileName: RtmpPushService
 * Author: FlyZebra
 * Email:flycnzebra@gmail.com
 * Date: 2023/6/23 11:07
 * Description:
 */
package com.flyzebra.mdvr.camera;

import com.flyzebra.mdvr.Config;
import com.flyzebra.media.VideoEncoder;
import com.flyzebra.media.VideoEncoderCB;
import com.flyzebra.notify.INotify;
import com.flyzebra.notify.Notify;
import com.flyzebra.notify.NotifyType;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class AvcService implements VideoEncoderCB, INotify {
    public static int AVC = 1;
    public static int HEVC = 2;
    private final int mChannel;

    int yuvSize = Config.CAM_WIDTH * Config.CAM_HEIGHT * 3 / 2;
    int yuvBufSize = yuvSize + 8;
    private final ByteBuffer yuvBuf = ByteBuffer.wrap(new byte[yuvBufSize * 15]);
    private Thread yuvThread;
    private final Object yuvLock = new Object();

    private AtomicBoolean is_stop = new AtomicBoolean(true);

    public AvcService(int channel) {
        mChannel = channel;
    }

    public void onCreate() {
        FlyLog.d("AvcService[%d] start!", mChannel);
        Notify.get().registerListener(this);
        is_stop.set(false);
        yuvThread = new Thread(() -> {
            long ptsUsec = 0;
            byte[] yuv = new byte[yuvSize];
            VideoEncoder videoEncoder = new VideoEncoder(mChannel, this);
            videoEncoder.initCodec(Config.CAM_MIME_TYPE, Config.CAM_WIDTH, Config.CAM_HEIGHT, Config.CAM_BIT_RATE);
            while (!is_stop.get()) {
                synchronized (yuvLock) {
                    if (yuvBuf.position() < yuvBufSize) {
                        try {
                            yuvLock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        yuvBuf.flip();
                        ptsUsec = yuvBuf.getLong();
                        yuvBuf.get(yuv);
                        yuvBuf.compact();
                    }
                }
                if (is_stop.get()) break;
                videoEncoder.inYuvData(yuv, yuvSize, ptsUsec);
            }
            videoEncoder.releaseCodec();
        }, "yuv-" + mChannel);
        yuvThread.start();
    }

    public void onDistory() {
        FlyLog.d("AvcService[%d] will exit!", mChannel);
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
        FlyLog.d("AvcService[%d] exit!", mChannel);
    }

    @Override
    public void notifySpsPps(int channel, byte[] data, int size) {
        byte[] params = new byte[4];
        ByteUtil.shortToBytes((short) channel, params, 0, true);
        ByteUtil.shortToBytes((short) AVC, params, 2, true);//Format AVC
        Notify.get().handledata(NotifyType.NOTI_CAMOUT_SPS, data, size, params);
    }

    @Override
    public void notifyVpsSpsPps(int channel, byte[] data, int size) {
        byte[] params = new byte[4];
        ByteUtil.shortToBytes((short) channel, params, 0, true);
        ByteUtil.shortToBytes((short) HEVC, params, 2, true);//Format HEVC
        Notify.get().handledata(NotifyType.NOTI_CAMOUT_SPS, data, size, params);
    }

    @Override
    public void notifyAvcData(int channel, byte[] data, int size, long pts) {
        byte[] params = new byte[12];
        ByteUtil.shortToBytes((short) channel, params, 0, true);
        ByteUtil.longToBytes(pts, params, 2, true);
        ByteUtil.shortToBytes((short) AVC, params, 10, true);//Format AVC
        Notify.get().handledata(NotifyType.NOTI_CAMOUT_AVC, data, size, params);
    }

    @Override
    public void notifyHevcData(int channel, byte[] data, int size, long pts) {
        byte[] params = new byte[12];
        ByteUtil.shortToBytes((short) channel, params, 0, true);
        ByteUtil.longToBytes(pts, params, 2, true);
        ByteUtil.shortToBytes((short) HEVC, params, 10, true);//Format HEVC
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
            long ptsUsec = ByteUtil.bytes2Long(params, 6, true);
            synchronized (yuvLock) {
                try {
                    if (yuvBuf.remaining() < yuvBufSize) {
                        FlyLog.e("Yuv Buffer is full, clean all buffer!");
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
