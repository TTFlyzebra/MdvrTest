/**
 * FileName: RtmpPushService
 * Author: FlyZebra
 * Email:flycnzebra@gmail.com
 * Date: 2023/6/23 11:07
 * Description:
 */
package com.flyzebra.mdvr.rtmp;

import com.flyzebra.core.notify.INotify;
import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.NotifyType;
import com.flyzebra.mdvr.Config;
import com.flyzebra.mdvr.camera.CameraEncoder;
import com.flyzebra.rtmp.RtmpDump;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class RtmpPusher implements INotify {
    private final int mChannel;
    private final AtomicBoolean is_stop = new AtomicBoolean(true);
    private final AtomicBoolean is_rtmp = new AtomicBoolean(false);
    private final ByteBuffer sendBuf = ByteBuffer.wrap(new byte[Config.CAM_WIDTH * Config.CAM_HEIGHT * 10]);
    private Thread sendThread;
    private final Object sendLock = new Object();
    private byte[] videoHead = null;
    private byte[] audioHead = null;

    public RtmpPusher(int channel) {
        mChannel = channel;
    }

    public void start(String rtmp_url) {
        FlyLog.d("PusherService[%d] start !", mChannel);
        Notify.get().registerListener(this);
        is_stop.set(false);
        sendThread = new Thread(() -> {
            boolean is_send_audio_head = false;
            boolean is_send_video_head = false;
            int type = 0;
            int size = 0;
            int paramsLen;
            byte[] data = new byte[Config.CAM_WIDTH * Config.CAM_HEIGHT * 3 / 2];
            byte[] params = new byte[1024];
            RtmpDump rtmp = new RtmpDump();
            while (!is_stop.get()) {
                if (!is_rtmp.get()) {
                    is_send_audio_head = false;
                    is_send_video_head = false;
                    boolean flag = rtmp.open(mChannel, rtmp_url);
                    is_rtmp.set(flag);
                    if (is_stop.get()) break;
                    if (!is_rtmp.get()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        continue;
                    }
                }

                synchronized (sendLock) {
                    if (sendBuf.position() <= 0) {
                        try {
                            sendLock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (is_stop.get()) break;
                    sendBuf.flip();
                    type = sendBuf.getInt();
                    size = sendBuf.getInt();
                    sendBuf.get(data, 0, size);
                    paramsLen = sendBuf.getInt();
                    sendBuf.get(params, 0, paramsLen);
                    sendBuf.compact();
                }

                boolean flag = true;
                if (NotifyType.NOTI_CAMOUT_AVC == type) {
                    long pts = ByteUtil.bytes2Long(params, 2, true);
                    short format = ByteUtil.bytes2Short(params, 10, true);
                    if (!is_send_video_head && videoHead != null) {
                        if (format == CameraEncoder.AVC) {
                            flag = sendAvcHead(rtmp, videoHead, videoHead.length);
                            if (flag) is_send_video_head = true;
                        } else {
                            flag = sendHevcHead(rtmp, videoHead, videoHead.length);
                            if (flag) is_send_video_head = true;
                        }
                    }
                    if (is_send_video_head) {
                        if (format == CameraEncoder.AVC) {
                            flag = sendAvcData(rtmp, data, size, pts);
                        } else {
                            flag = sendHevcData(rtmp, data, size, pts);
                        }
                    }
                } else if (NotifyType.NOTI_SNDOUT_AAC == type) {
                    if (!is_send_audio_head && audioHead != null) {
                        if (sendAacHead(rtmp, audioHead, audioHead.length))
                            is_send_audio_head = true;
                    }
                    long pts = ByteUtil.bytes2Long(params, 2, true);
                    flag = sendAacData(rtmp, data, size, pts);
                }
                if (!flag) {
                    rtmp.close();
                    is_rtmp.set(false);
                }
            }
            if (is_rtmp.get()) {
                rtmp.close();
                is_rtmp.set(false);
            }
        }, "rtmp-push" + mChannel);
        sendThread.start();
    }

    public void stop() {
        is_stop.set(true);
        Notify.get().unregisterListener(this);
        synchronized (sendLock) {
            sendLock.notifyAll();
        }
        try {
            sendThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        FlyLog.d("PusherService[%d] exit !", mChannel);
    }

    private boolean sendAacHead(RtmpDump rtmp, byte[] head, int headLen) {
        return rtmp.writeAacHead(head, headLen);
    }

    private boolean sendAacData(RtmpDump rtmp, byte[] data, int size, long pts) {
        return rtmp.writeAacData(data, size, pts);
    }

    private boolean sendAvcHead(RtmpDump rtmp, byte[] data, int size) {
        return rtmp.writeAvcHead(data, size);
    }

    private boolean sendAvcData(RtmpDump rtmp, byte[] data, int size, long pts) {
        return rtmp.writeAvcData(data, size, pts);
    }

    private boolean sendHevcHead(RtmpDump rtmp, byte[] data, int size) {
        return rtmp.writeHevcHead(data, size);
    }

    private boolean sendHevcData(RtmpDump rtmp, byte[] data, int size, long pts) {
        return rtmp.writeHevcData(data, size, pts);
    }

    @Override
    public void notify(byte[] data, int size) {

    }

    @Override
    public void handle(int type, byte[] data, int size, byte[] params) {
        if (NotifyType.NOTI_SNDOUT_SPS == type) {
            short channel = ByteUtil.bytes2Short(params, 0, true);
            if (mChannel != channel) return;
            audioHead = new byte[size];
            System.arraycopy(data, 0, audioHead, 0, size);
        } else if (NotifyType.NOTI_CAMOUT_SPS == type) {
            short channel = ByteUtil.bytes2Short(params, 0, true);
            if (mChannel != channel) return;
            videoHead = new byte[size];
            System.arraycopy(data, 0, videoHead, 0, size);
        } else if (NotifyType.NOTI_SNDOUT_AAC == type || NotifyType.NOTI_CAMOUT_AVC == type) {
            short channel = ByteUtil.bytes2Short(params, 0, true);
            if (mChannel != channel) return;
            if (!is_rtmp.get()) return;
            synchronized (sendLock) {
                if (sendBuf.remaining() < (4 + 4 + size + params.length)) {
                    FlyLog.e("rtmp send buffer[%d] is full, clean all buffer!", mChannel);
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
