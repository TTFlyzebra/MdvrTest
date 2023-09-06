package com.flyzebra.mdvr.record;

import android.os.SystemClock;
import android.text.TextUtils;

import com.flyzebra.core.media.ZebraMuxer;
import com.flyzebra.core.notify.INotify;
import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.NotifyType;
import com.flyzebra.mdvr.Config;
import com.flyzebra.mdvr.Global;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecordTasker implements INotify {
    private final int mChannel;
    private final AtomicBoolean is_stop = new AtomicBoolean(true);
    private final ByteBuffer saveBuf = ByteBuffer.allocateDirect(2 * 1024 * 1024);
    private final Object saveLock = new Object();
    private Thread saveThread;
    private int videoHeadSize = 16;
    private int audioHeadSize = 16;

    public RecordTasker(int channel) {
        mChannel = channel;
    }

    public void onCreate(RecordService service) {
        if (service == null) return;
        FlyLog.d("StorageTasker[%d] start !", mChannel);
        Notify.get().registerListener(this);

        is_stop.set(false);
        saveThread = new Thread(() -> {
            int dataSize = 0;
            int mallocSize = 1024 * 1024;
            byte[] data = new byte[mallocSize];
            ZebraMuxer muxer = null;
            long lastCount = 0;
            while (!is_stop.get()) {
                synchronized (saveLock) {
                    if (saveBuf.position() <= 0) {
                        try {
                            saveLock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (is_stop.get()) break;
                    saveBuf.flip();
                    dataSize = saveBuf.limit();
                    if (mallocSize < dataSize) {
                        mallocSize = dataSize;
                        data = new byte[mallocSize];
                    }
                    saveBuf.get(data, 0, dataSize);
                    saveBuf.compact();
                }
                int pos = 0;
                while (pos < dataSize) {
                    int size = ByteUtil.bytes2Int(data, pos, true);
                    int type = ByteUtil.bytes2Int(data, 4 + pos, true);
                    long count = System.currentTimeMillis() / Config.RECORD_TIME;
                    boolean is_newfile = false;
                    if (type == ZebraMuxer.VIDEO_FRAME && count > lastCount && (data[pos + videoHeadSize] & 0x1f) != 1) {
                        lastCount = count;
                        is_newfile = true;
                        if (muxer != null) muxer.close(true);
                    }
                    if (is_newfile) {
                        String fileName = service.getSaveFileName(mChannel);
                        if (TextUtils.isEmpty(fileName)) {
                            FlyLog.e("create recored filename failed！");
                            return;
                        } else {
                            FlyLog.d("create new recored file %s", fileName);
                        }
                        muxer = new ZebraMuxer(fileName);
                        byte[] videoHead = Global.videoHeadMap.get(mChannel);
                        byte[] audioHead = Global.audioHeadMap.get(mChannel);
                        muxer.addVideoTrack(videoHead, videoHead.length);
                        if (audioHead != null) muxer.addAudioTrack(audioHead, audioHead.length);
                    }
                    if (muxer != null) {
                        if (type == ZebraMuxer.VIDEO_FRAME) {
                            muxer.writeVideoFrame(data, pos, size);
                        } else if (type == ZebraMuxer.AUDIO_FRAME) {
                            muxer.writeAudioFrame(data, pos, size);
                        }
                    }
                    pos += size;
                }
            }
            if (muxer != null) {
                muxer.close(false);
            }
        }, "save_task" + mChannel);
        saveThread.start();
    }

    public void onDestory() {
        is_stop.set(true);
        Notify.get().unregisterListener(this);
        synchronized (saveLock) {
            saveLock.notifyAll();
        }
        try {
            saveThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        FlyLog.d("StorageTasker[%d] exit !", mChannel);
    }

    @Override
    public void notify(byte[] data, int size) {

    }

    @Override
    public void handle(int type, byte[] data, int size, byte[] params) {
        long stime = SystemClock.uptimeMillis();
        if (NotifyType.NOTI_CAMOUT_AVC == type) {
            short channel = ByteUtil.bytes2Short(params, 0, true);
            if (mChannel != channel) return;
            long pts = ByteUtil.bytes2Long(params, 2, true);
            synchronized (saveLock) {
                if (saveBuf.remaining() < size + videoHeadSize) {
                    FlyLog.e("avc save buffer[%d] is full, clean all buffer!", mChannel);
                    saveBuf.clear();
                }
                byte[] head = new byte[videoHeadSize];
                ByteUtil.intToBytes(size + videoHeadSize, head, 0, true);
                ByteUtil.intToBytes(ZebraMuxer.VIDEO_FRAME, head, 4, true);
                ByteUtil.longToBytes(pts, head, 8, true);
                saveBuf.put(head, 0, 8);
                saveBuf.put(data, 0, size);
                saveBuf.put(head, 8, 8);
                saveLock.notify();
            }
        } else if (NotifyType.NOTI_MICOUT_AAC == type) {
            short channel = ByteUtil.bytes2Short(params, 0, true);
            if (mChannel != channel) return;
            long pts = ByteUtil.bytes2Long(params, 2, true);
            synchronized (saveLock) {
                if (saveBuf.remaining() < size + audioHeadSize) {
                    FlyLog.e("aac save buffer[%d] is full, clean all buffer!", mChannel);
                    saveBuf.clear();
                }
                byte[] head = new byte[audioHeadSize];
                ByteUtil.intToBytes(size + audioHeadSize, head, 0, true);
                ByteUtil.intToBytes(ZebraMuxer.AUDIO_FRAME, head, 4, true);
                ByteUtil.longToBytes(pts, head, 8, true);
                saveBuf.put(head, 0, 8);
                saveBuf.put(data, 0, size);
                saveBuf.put(head, 8, 8);
                saveLock.notify();
            }
        }
        long utime = SystemClock.uptimeMillis() - stime;
        if (utime > 50) {
            FlyLog.e("RecordTasker handle use type %d, time %d, size %d", type, utime, size);
        }
    }
}
