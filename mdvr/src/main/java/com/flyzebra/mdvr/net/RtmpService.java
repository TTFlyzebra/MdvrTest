/**
 * FileName: RtmpPushService
 * Author: FlyZebra
 * Email:flycnzebra@gmail.com
 * Date: 2023/6/23 11:07
 * Description:
 */
package com.flyzebra.mdvr.net;

import com.flyzebra.mdvr.Config;
import com.flyzebra.media.AudioEncoder;
import com.flyzebra.media.AudioEncoderCB;
import com.flyzebra.media.VideoEncoder;
import com.flyzebra.media.VideoEncoderCB;
import com.flyzebra.notify.INotify;
import com.flyzebra.notify.Notify;
import com.flyzebra.notify.NotifyType;
import com.flyzebra.rtmp.RtmpDump;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class RtmpService implements VideoEncoderCB, AudioEncoderCB, INotify {
    private final int mChannel;
    private final RtmpDump rtmpDump;

    int pcmSize = (int) (Config.MIC_SAMPLE * 1.0f * 16 / 8 * 1 / 25.0f);
    int pcmBufSize = pcmSize + 8;
    private final ByteBuffer pcmBuf = ByteBuffer.wrap(new byte[pcmBufSize * 15]);
    private Thread pcmThread;
    private final Object pcmLock = new Object();

    int yuvSize = Config.CAM_WIDTH * Config.CAM_HEIGHT * 3 / 2;
    int yuvBufSize = yuvSize + 8;
    private final ByteBuffer yuvBuf = ByteBuffer.wrap(new byte[yuvBufSize * 15]);
    private Thread yuvThread;
    private final Object yuvLock = new Object();

    private AtomicBoolean is_stop = new AtomicBoolean(true);

    public RtmpService(int channel) {
        mChannel = channel;
        rtmpDump = new RtmpDump(channel);
    }

    public void start(String rtmp_url) {
        FlyLog.d("rtmpDump start channel=%d, rtmp_url=%s", mChannel, rtmp_url);
        Notify.get().registerListener(this);
        rtmpDump.init(rtmp_url);

        is_stop.set(false);
        pcmThread = new Thread(() -> {
            long ptsUsec = 0;
            byte[] pcm = new byte[pcmSize];
            AudioEncoder audioEncoder = new AudioEncoder(mChannel, this);
            audioEncoder.initCodec(Config.MIC_MIME_TYPE, Config.MIC_SAMPLE, Config.MIC_CHANNELS, Config.MIC_BIT_RATE);
            while (!is_stop.get()) {
                synchronized (pcmLock) {
                    if (pcmBuf.position() < pcmBufSize) {
                        try {
                            pcmLock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        pcmBuf.flip();
                        ptsUsec = pcmBuf.getLong();
                        pcmBuf.get(pcm);
                        pcmBuf.compact();
                    }
                }
                if (is_stop.get()) break;
                audioEncoder.inPumData(pcm, pcmSize, ptsUsec);
            }
            audioEncoder.releaseCodec();
        }, "pcm-" + mChannel);
        pcmThread.start();

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

    public void stop() {
        Notify.get().unregisterListener(this);
        is_stop.set(true);
        synchronized (pcmLock) {
            pcmLock.notifyAll();
        }
        synchronized (yuvLock) {
            yuvLock.notifyAll();
        }
        try {
            pcmThread.join();
            yuvThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        rtmpDump.release();
    }

    @Override
    public void notifySpsPps(int channel, byte[] data, int size) {
        try {
            int sps_p = -1;
            int pps_p = -1;
            for (int i = 0; i < size; i++) {
                if (data[i] == 0x00 && data[i + 1] == 0x00 && data[i + 2] == 0x00 && data[i + 3] == 0x01) {
                    if (sps_p == -1) {
                        sps_p = i;
                        i += 3;
                    } else {
                        pps_p = i;
                        break;
                    }
                }
            }
            if (sps_p == -1 || pps_p == -1) {
                FlyLog.e("Get sps pps error!");
                return;
            }
            int spsLen = pps_p - 4;
            byte[] sps = new byte[spsLen];
            System.arraycopy(data, 4, sps, 0, spsLen);
            int ppsLen = size - pps_p - 4;
            byte[] pps = new byte[ppsLen];
            System.arraycopy(data, pps_p + 4, pps, 0, ppsLen);
            rtmpDump.sendSpsPps(sps, spsLen, pps, ppsLen);
        } catch (Exception e) {
            FlyLog.e(e.toString());
        }
    }

    @Override
    public void notifyVpsSpsPps(int channel, byte[] data, int size) {
        try {
            int vps_p = -1;
            int sps_p = -1;
            int pps_p = -1;
            for (int i = 0; i < size; i++) {
                if (data[i] == 0x00 && data[i + 1] == 0x00 && data[i + 2] == 0x00 && data[i + 3] == 0x01) {
                    if (vps_p == -1) {
                        vps_p = i;
                        i += 3;
                    } else if (sps_p == -1) {
                        sps_p = i;
                        i += 3;
                    } else {
                        pps_p = i;
                        break;
                    }
                }
            }
            if (vps_p == -1 || sps_p == -1 || pps_p == -1) {
                FlyLog.e("Get vps sps pps error!");
                return;
            }
            int vpsLen = sps_p - 4;
            byte[] vps = new byte[vpsLen];
            System.arraycopy(data, 4, vps, 0, vpsLen);
            int spsLen = pps_p - sps_p - 4;
            byte[] sps = new byte[spsLen];
            System.arraycopy(data, sps_p + 4, sps, 0, spsLen);
            int ppsLen = size - pps_p - 4;
            byte[] pps = new byte[ppsLen];
            System.arraycopy(data, pps_p + 4, pps, 0, ppsLen);
            rtmpDump.sendVpsSpsPps(vps, vpsLen, sps, spsLen, pps, ppsLen);
        } catch (Exception e) {
            FlyLog.e(e.toString());
        }
    }

    @Override
    public void notifyAvcData(int channel, byte[] data, int size, long pts) {
        rtmpDump.sendAvc(data, size, pts);
    }

    @Override
    public void notifyHevcData(int channel, byte[] data, int size, long pts) {
        rtmpDump.sendHevc(data, size, pts);
    }

    @Override
    public void notifyAacHead(int channel, byte[] head, int headLen) {
        rtmpDump.sendAacHead(head, headLen);
    }

    @Override
    public void notifyAacData(int channel, byte[] data, int size, long pts) {
        rtmpDump.sendAac(data, size, pts);
    }

    @Override
    public void notify(byte[] data, int size) {

    }

    @Override
    public void handle(int type, byte[] data, int size, byte[] params) {
        if (NotifyType.NOTI_MICOUT_PCM == type) {
            int channel = ByteUtil.bytes2Short(params, 0, true);
            long ptsUsec = ByteUtil.bytes2Long(params, 12, true);
            if (this.mChannel != channel) return;
            synchronized (pcmLock) {
                try {
                    if (pcmBuf.remaining() < pcmBufSize) {
                        FlyLog.e("PcmBuf[%d] is full, clean all buffer!", mChannel);
                        pcmBuf.clear();
                    }
                    pcmBuf.putLong(ptsUsec);
                    pcmBuf.put(data);
                    pcmLock.notify();
                } catch (Exception e) {
                    FlyLog.e(e.toString());
                }
            }
        } else if (NotifyType.NOTI_CAMOUT_YUV == type) {
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
                    yuvLock.notifyAll();
                } catch (Exception e) {
                    FlyLog.e(e.toString());
                }
            }
        }
    }
}
