/**
 * FileName: RtmpPushService
 * Author: FlyZebra
 * Email:flycnzebra@gmail.com
 * Date: 2023/6/23 11:07
 * Description:
 */
package com.flyzebra.mdvr.rtmp;

import com.flyzebra.mdvr.Config;
import com.flyzebra.mdvr.camera.AvcService;
import com.flyzebra.notify.INotify;
import com.flyzebra.notify.Notify;
import com.flyzebra.notify.NotifyType;
import com.flyzebra.rtmp.RtmpDump;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class PusherService implements INotify {
    private final int mChannel;
    private final RtmpDump rtmpDump;

    private AtomicBoolean is_stop = new AtomicBoolean(true);

    private final ByteBuffer sendBuf = ByteBuffer.wrap(new byte[Config.CAM_WIDTH * Config.CAM_HEIGHT * 10]);
    private Thread sendThread;
    private final Object sendLock = new Object();

    public PusherService(int channel) {
        mChannel = channel;
        rtmpDump = new RtmpDump(channel);
    }

    public void onCreate(String rtmp_url) {
        FlyLog.d("RtmpPusherService[%d][%s] start !", mChannel, rtmp_url);
        Notify.get().registerListener(this);
        rtmpDump.init(rtmp_url);
        is_stop.set(false);
        sendThread = new Thread(() -> {
            int type = 0;
            int size = 0;
            byte[] data = new byte[Config.CAM_WIDTH * Config.CAM_HEIGHT * 3 / 2];
            int paramsLen;
            byte[] params = new byte[1024];
            while (!is_stop.get()) {
                synchronized (sendLock) {
                    if (sendBuf.position() <= 0) {
                        try {
                            sendLock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        sendBuf.flip();
                        type = sendBuf.getInt();
                        size = sendBuf.getInt();
                        sendBuf.get(data, 0, size);
                        paramsLen = sendBuf.getInt();
                        sendBuf.get(params, 0, paramsLen);
                        sendBuf.compact();
                    }
                }
                if (is_stop.get()) break;
                handleSendData(type, data, size, params);
            }
        }, "send-" + mChannel);
        sendThread.start();
    }

    private void handleSendData(int type, byte[] data, int size, byte[] params) {
        if (NotifyType.NOTI_SNDOUT_SPS == type) {
            short channel = ByteUtil.bytes2Short(params, 0, true);
            if (mChannel != channel) return;
            sendAacHead(channel, data, size);
        } else if (NotifyType.NOTI_SNDOUT_AAC == type) {
            short channel = ByteUtil.bytes2Short(params, 0, true);
            if (mChannel != channel) return;
            long pts = ByteUtil.bytes2Long(params, 2, true);
            sendAacData(channel, data, size, pts);
        } else if (NotifyType.NOTI_CAMOUT_SPS == type) {
            short channel = ByteUtil.bytes2Short(params, 0, true);
            if (mChannel != channel) return;
            short format = ByteUtil.bytes2Short(params, 2, true);
            if (format == AvcService.AVC) {
                sendAvcHead(channel, data, size);
            } else {
                sendHevcHead(channel, data, size);
            }
        } else if (NotifyType.NOTI_CAMOUT_AVC == type) {
            short channel = ByteUtil.bytes2Short(params, 0, true);
            if (mChannel != channel) return;
            long pts = ByteUtil.bytes2Long(params, 2, true);
            short format = ByteUtil.bytes2Short(params, 10, true);
            if (format == AvcService.AVC) {
                sendAvcData(channel, data, size, pts);
            } else {
                sendHevcData(channel, data, size, pts);
            }
        }
    }

    public void onDestory() {
        FlyLog.d("PusherService[%d] will exit !", mChannel);
        Notify.get().unregisterListener(this);
        is_stop.set(true);
        synchronized (sendLock) {
            sendLock.notifyAll();
        }
        try {
            sendThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        rtmpDump.release();
        FlyLog.d("PusherService[%d] exit !", mChannel);
    }

    private void sendAacHead(int channel, byte[] head, int headLen) {
        rtmpDump.sendAacHead(head, headLen);
    }

    private void sendAacData(int channel, byte[] data, int size, long pts) {
        rtmpDump.sendAac(data, size, pts);
    }

    private void sendAvcHead(int channel, byte[] data, int size) {
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

    private void sendHevcHead(int channel, byte[] data, int size) {
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

    private void sendAvcData(int channel, byte[] data, int size, long pts) {
        rtmpDump.sendAvc(data, size, pts);
    }

    private void sendHevcData(int channel, byte[] data, int size, long pts) {
        rtmpDump.sendHevc(data, size, pts);
    }

    @Override
    public void notify(byte[] data, int size) {

    }

    @Override
    public void handle(int type, byte[] data, int size, byte[] params) {
        if (NotifyType.NOTI_SNDOUT_SPS == type || NotifyType.NOTI_SNDOUT_AAC == type ||
                NotifyType.NOTI_CAMOUT_SPS == type || NotifyType.NOTI_CAMOUT_AVC == type) {
            synchronized (sendLock) {
                if (sendBuf.remaining() < (4 + 4 + size + params.length)) {
                    FlyLog.e("rtmp send buffer is full, clean all buffer!");
                    sendBuf.clear();
                }
                sendBuf.putInt(type);
                sendBuf.putInt(size);
                sendBuf.put(data, 0, size);
                sendBuf.putInt(params.length);
                sendBuf.put(params);
                sendLock.notify();
            }
        }
    }
}
