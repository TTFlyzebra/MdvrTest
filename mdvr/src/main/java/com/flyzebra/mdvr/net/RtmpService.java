/**
 * FileName: RtmpPushService
 * Author: FlyZebra
 * Email:flycnzebra@gmail.com
 * Date: 2023/6/23 11:07
 * Description:
 */
package com.flyzebra.mdvr.net;

import android.os.Handler;
import android.os.HandlerThread;

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

public class RtmpService implements VideoEncoderCB, AudioEncoderCB, INotify {
    private final int channel;
    private final VideoEncoder videoEncoder;
    private final AudioEncoder audioEncoder;
    private final RtmpDump rtmpDump;

    private static final HandlerThread mRtmpThread = new HandlerThread("rtmp_service");

    static {
        mRtmpThread.start();
    }

    private static final Handler tHandler = new Handler(mRtmpThread.getLooper());

    public RtmpService(int channel) {
        this.channel = channel;
        rtmpDump = new RtmpDump(channel);
        videoEncoder = new VideoEncoder(channel, this);
        audioEncoder = new AudioEncoder(channel, this);
    }

    public void start(String rtmp_url) {
        rtmpDump.init(rtmp_url);
        FlyLog.d("rtmpDump init channel=%d, rtmp_url=%s", channel, rtmp_url);
        Notify.get().registerListener(this);
    }

    public void stop() {
        Notify.get().unregisterListener(this);
        rtmpDump.release();
        videoEncoder.releaseCodec();
        audioEncoder.releaseCodec();
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
        rtmpDump.sendAvc(data, size, pts/1000);
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
        rtmpDump.sendAac(data, size, pts/1000);
    }

    @Override
    public void notify(byte[] data, int size) {

    }

    private static final Object codecLocked = new Object();

    @Override
    public void handle(int type, byte[] data, int size, byte[] params) {
        if (NotifyType.NOTI_CAMOUT_YUV == type) {
            int channel = ByteUtil.bytes2Short(params, 0, true);
            if (this.channel != channel) return;
            int width = ByteUtil.bytes2Short(params, 2, true);
            int height = ByteUtil.bytes2Short(params, 4, true);
            long ptsUsec = ByteUtil.bytes2Long(params, 6, true);
            synchronized (codecLocked) {
                if (!videoEncoder.isCodecInit()) {
                    videoEncoder.initCodec(Config.CAM_MIME_TYPE, width, height, Config.CAM_BIT_RATE);
                }
            }
            videoEncoder.inYuvData(data, size, ptsUsec);
        } else if (NotifyType.NOTI_MICOUT_PCM == type) {
            int channel = ByteUtil.bytes2Short(params, 0, true);
            if (this.channel != channel) return;
            int sample = ByteUtil.bytes2Int(params, 2, true);
            int channels = ByteUtil.bytes2Short(params, 6, true);
            int bitrate = ByteUtil.bytes2Int(params, 8, true);
            synchronized (codecLocked) {
                if (!audioEncoder.isCodecInit()) {
                    audioEncoder.initCodec(Config.MIC_MIME_TYPE, sample, channels, bitrate);
                }
            }
            audioEncoder.inPumData(data, size, System.nanoTime() / 1000);
        }
    }
}
