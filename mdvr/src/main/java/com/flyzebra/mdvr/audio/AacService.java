/**
 * FileName: RtmpPushService
 * Author: FlyZebra
 * Email:flycnzebra@gmail.com
 * Date: 2023/6/23 11:07
 * Description:
 */
package com.flyzebra.mdvr.audio;

import com.flyzebra.core.media.AudioEncoder;
import com.flyzebra.core.media.AudioEncoderCB;
import com.flyzebra.core.notify.INotify;
import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.NotifyType;
import com.flyzebra.mdvr.Config;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class AacService implements AudioEncoderCB, INotify {
    private final int mChannel;

    int pcmSize = (int) (Config.MIC_SAMPLE * 1.0f * 16 / 8 * 2 / 25.0f);
    int pcmBufSize = pcmSize + 8;
    private final ByteBuffer pcmBuf = ByteBuffer.wrap(new byte[pcmBufSize * 15]);
    private Thread pcmThread;
    private final Object pcmLock = new Object();

    private AtomicBoolean is_stop = new AtomicBoolean(true);

    public AacService(int channel) {
        mChannel = channel;
    }

    public void onCreate() {
        FlyLog.d("AacService[%d] start!", mChannel);
        Notify.get().registerListener(this);
        is_stop.set(false);
        pcmThread = new Thread(() -> {
            long ptsUsec = 0;
            byte[] pcm = new byte[pcmSize];
            AudioEncoder audioEncoder = new AudioEncoder(mChannel, this);
            audioEncoder.initCodec(Config.MIC_MIME_TYPE, Config.MIC_SAMPLE, Config.MIC_CHANNELS, Config.MIC_BIT_RATE);
            while (!is_stop.get()) {
                synchronized (pcmLock) {
                    if (!is_stop.get() && pcmBuf.position() < pcmBufSize) {
                        try {
                            pcmLock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (is_stop.get()) break;
                    pcmBuf.flip();
                    ptsUsec = pcmBuf.getLong();
                    pcmBuf.get(pcm);
                    pcmBuf.compact();
                }
                audioEncoder.inPumData(pcm, pcmSize, ptsUsec);
            }
            audioEncoder.releaseCodec();
        }, "audio-aac" + mChannel);
        pcmThread.start();
    }

    public void onDistory() {
        is_stop.set(true);
        Notify.get().unregisterListener(this);
        synchronized (pcmLock) {
            pcmLock.notifyAll();
        }
        try {
            pcmThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        FlyLog.d("AacService[%d] exit!", mChannel);
    }

    @Override
    public void notifyAacHead(int channel, byte[] head, int headLen) {
        byte[] params = new byte[2];
        ByteUtil.shortToBytes((short) channel, params, 0, true);
        Notify.get().handledata(NotifyType.NOTI_SNDOUT_SPS, head, headLen, params);
    }

    @Override
    public void notifyAacData(int channel, byte[] data, int size, long pts) {
        byte[] params = new byte[10];
        ByteUtil.shortToBytes((short) channel, params, 0, true);
        ByteUtil.longToBytes(pts, params, 2, true);
        Notify.get().handledata(NotifyType.NOTI_SNDOUT_AAC, data, size, params);
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
        }
    }
}
