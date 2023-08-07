package com.flyzebra.mdvr.store;

import android.text.TextUtils;

import com.flyzebra.core.notify.INotify;
import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.NotifyType;
import com.flyzebra.mdvr.Global;
import com.flyzebra.mdvr.model.ZebraMuxer;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class StorageTasker implements INotify {
    private final int mChannel;
    private final AtomicBoolean is_stop = new AtomicBoolean(true);
    private final ByteBuffer saveBuf = ByteBuffer.wrap(new byte[1920 * 1080 * 2]);
    private final Object saveLock = new Object();
    private Thread saveThread;

    public StorageTasker(int channel) {
        mChannel = channel;
    }

    public void onCreate(StorageService service) {
        if (service == null) return;
        FlyLog.d("StorageTasker[%d] start !", mChannel);
        Notify.get().registerListener(this);

        is_stop.set(false);
        saveThread = new Thread(() -> {
            int type = 0;
            long pts = 0;
            int size = 0;
            int dataSize = 1280 * 720;
            byte[] data = new byte[dataSize];
            ZebraMuxer zebraMuxer = null;
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
                    type = saveBuf.getInt();
                    pts = saveBuf.getLong();
                    size = saveBuf.getInt();
                    if (dataSize < size) {
                        dataSize = size;
                        data = new byte[dataSize];
                    }
                    saveBuf.get(data, 8, size);
                    saveBuf.compact();
                }
                long count = System.currentTimeMillis() / (1000 * 300);
                boolean is_newfile = false;
                if (type == 1 && count > lastCount && (data[0] & 0x1f) != 1) {
                    lastCount = count;
                    is_newfile = true;
                    if (zebraMuxer != null) zebraMuxer.close();
                }
                if (is_newfile) {
                    String fileName = service.getSaveFileName(mChannel);
                    if (TextUtils.isEmpty(fileName)) {
                        FlyLog.e("get recored filename failed！");
                        return;
                    }
                    zebraMuxer = new ZebraMuxer(fileName);
                    byte[] videoHead = Global.videoHeadMap.get(mChannel);
                    byte[] audioHead = Global.audioHeadMap.get(mChannel);
                    zebraMuxer.addVideoTrack(videoHead, videoHead.length);
                    zebraMuxer.addAudioTrack(audioHead, audioHead.length);
                }
                if (zebraMuxer != null) {
                    if (type == 1) {
                        zebraMuxer.writeVideoFrame(data, size, pts);
                    } else {
                        zebraMuxer.writeAudioFrame(data, size, pts);
                    }
                }
            }
            if (zebraMuxer != null) {
                zebraMuxer.close();
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
        if (NotifyType.NOTI_CAMOUT_AVC == type) {
            short channel = ByteUtil.bytes2Short(params, 0, true);
            if (mChannel != channel) return;
            long pts = ByteUtil.bytes2Long(params, 2, true);
            synchronized (saveLock) {
                if (saveBuf.remaining() < size) {
                    FlyLog.e("save buffer[%d] is full, clean all buffer!", mChannel);
                    saveBuf.clear();
                }
                saveBuf.putInt(1);
                saveBuf.putLong(pts);
                saveBuf.putInt(size);
                saveBuf.put(data, 0, size);
                saveLock.notify();
            }
        } else if (NotifyType.NOTI_MICOUT_AAC == type) {
            short channel = ByteUtil.bytes2Short(params, 0, true);
            if (mChannel != channel) return;
            long pts = ByteUtil.bytes2Long(params, 2, true);
            synchronized (saveLock) {
                if (saveBuf.remaining() < size) {
                    FlyLog.e("save buffer[%d] is full, clean all buffer!", mChannel);
                    saveBuf.clear();
                }
                saveBuf.putInt(2);
                saveBuf.putLong(pts);
                saveBuf.putInt(size);
                saveBuf.put(data, 0, size);
                saveLock.notify();
            }
        }
    }
}
