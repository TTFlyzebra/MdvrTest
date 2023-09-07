/**
 * FileName: RtmpPushService
 * Author: FlyZebra
 * Email:flycnzebra@gmail.com
 * Date: 2023/6/23 11:07
 * Description:
 */
package com.flyzebra.mdvr.rtmp;

import android.media.MediaFormat;

import com.flyzebra.core.notify.INotify;
import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.NotifyType;
import com.flyzebra.mdvr.Config;
import com.flyzebra.mdvr.Global;
import com.flyzebra.rtmp.RtmpDump;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class RtmpPusher implements INotify {
    private final int mChannel;
    private final AtomicBoolean is_stop = new AtomicBoolean(true);
    private final AtomicBoolean is_rtmp = new AtomicBoolean(false);
    private final ByteBuffer sendBuf = ByteBuffer.wrap(new byte[1920 * 1080 * 2]);
    private Thread sendThread;
    private final Object sendLock = new Object();

    public RtmpPusher(int channel) {
        mChannel = channel;
    }

    public void onCreate(String rtmp_url) {
        FlyLog.d("RtmpPusher[%d] start !", mChannel);
        Notify.get().registerListener(this);
        is_stop.set(false);
        sendThread = new Thread(() -> {
            boolean is_send_audio_head = false;
            boolean is_send_video_head = false;
            int type = 0;
            int size = 0;
            int paramsLen;
            byte[] data = new byte[1920 * 1080 * 3 / 2];
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
                    if (!is_send_video_head) {
                        byte[] videoHead = Global.videoHeadMap.get(mChannel);
                        if (Config.CAM_MIME_TYPE.equals(MediaFormat.MIMETYPE_VIDEO_AVC)) {
                            flag = sendAvcHead(rtmp, videoHead, videoHead.length);
                            if (flag) is_send_video_head = true;
                        } else {
                            flag = sendHevcHead(rtmp, videoHead, videoHead.length);
                            if (flag) is_send_video_head = true;
                        }
                    }
                    if (is_send_video_head) {
                        if (Config.CAM_MIME_TYPE.equals(MediaFormat.MIMETYPE_VIDEO_AVC)) {
                            flag = sendAvcData(rtmp, data, size, pts);
                        } else {
                            flag = sendHevcData(rtmp, data, size, pts);
                        }
                    }
                } else if (NotifyType.NOTI_MICOUT_AAC == type) {
                    long pts = ByteUtil.bytes2Long(params, 2, true);
                    if (!is_send_audio_head) {
                        byte[] audioHead = Global.audioHeadMap.get(mChannel);
                        if (sendAacHead(rtmp, audioHead, audioHead.length))
                            is_send_audio_head = true;
                    }
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

    public void onDestory() {
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
        FlyLog.d("RtmpPusher[%d] exit !", mChannel);
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
    public void handle(int type, byte[] data, int dsize, byte[] params, int psize) {
        if (NotifyType.NOTI_MICOUT_AAC == type || NotifyType.NOTI_CAMOUT_AVC == type) {
            short channel = ByteUtil.bytes2Short(params, 0, true);
            if (mChannel != channel) return;
            if (!is_rtmp.get()) return;
            synchronized (sendLock) {
                if (sendBuf.remaining() < (4 + 4 + dsize + params.length)) {
                    FlyLog.e("rtmp send buffer[%d] is full, clean all buffer!", mChannel);
                    sendBuf.clear();
                }
                sendBuf.putInt(type);
                sendBuf.putInt(dsize);
                sendBuf.put(data, 0, dsize);
                sendBuf.putInt(params.length);
                sendBuf.put(params);
                sendLock.notify();
            }
        }
    }
}
