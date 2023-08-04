package com.flyzebra.mdvr.store;

import com.flyzebra.core.notify.INotify;
import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.NotifyType;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class StorageTasker implements INotify {
    private final int mChannel;
    private final AtomicBoolean is_stop = new AtomicBoolean(true);
    private final ByteBuffer saveBuf = ByteBuffer.wrap(new byte[1920 * 1080 * 2]);
    private final Object saveLock = new Object();
    private Thread saveThread;
    private byte[] videoHead = null;
    private byte[] audioHead = null;

    public StorageTasker(int channel) {
        mChannel = channel;
    }

    public void onCreate(final StorageTFcard tFcard) {
        if (tFcard == null) return;
        FlyLog.d("StorageTasker[%d] start !", mChannel);
        Notify.get().registerListener(this);

        is_stop.set(false);
        saveThread = new Thread(() -> {
            String savePath = tFcard.getPath() + File.separator + "MD201" + File.separator + "CHANNEL-" + mChannel;
            File file = new File(savePath);
            if (!file.exists()) {
                if (file.mkdirs()) {
                    FlyLog.e("create save file path %s failed!", savePath);
                    return;
                }
            }
            int dataSize = 1280 * 720;
            byte[] data = new byte[dataSize];
            byte type = 0;
            int size = 0;
            RandomAccessFile randomAccessFile = null;
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
                    type = saveBuf.get();
                    size = saveBuf.getInt();
                    if (dataSize < size) {
                        dataSize = size;
                        data = new byte[dataSize];
                    }
                    saveBuf.get(data, 0, size);
                    saveBuf.compact();
                }
                boolean is_newfile = false;
                long count = System.currentTimeMillis() / (1000 * 300);
                if (type == 0x01 && count > lastCount) {
                    lastCount = count;
                    is_newfile = true;
                }
                if (is_newfile) {
                    if (randomAccessFile != null) {
                        try {
                            randomAccessFile.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    try {
                        randomAccessFile = new RandomAccessFile(savePath + File.separator + System.currentTimeMillis(), "rws");
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (randomAccessFile != null) {
                    try {
                        randomAccessFile.write(data);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
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
        if (NotifyType.NOTI_MICOUT_SPS == type) {
            short channel = ByteUtil.bytes2Short(params, 0, true);
            if (mChannel != channel) return;
            audioHead = new byte[size];
            System.arraycopy(data, 0, audioHead, 0, size);
        } else if (NotifyType.NOTI_CAMOUT_SPS == type) {
            short channel = ByteUtil.bytes2Short(params, 0, true);
            if (mChannel != channel) return;
            videoHead = new byte[size];
            System.arraycopy(data, 0, videoHead, 0, size);
        } else if (NotifyType.NOTI_CAMOUT_AVC == type) {
            short channel = ByteUtil.bytes2Short(params, 0, true);
            if (mChannel != channel) return;
            synchronized (saveLock) {
                if (saveBuf.remaining() < size) {
                    FlyLog.e("save buffer[%d] is full, clean all buffer!", mChannel);
                    saveBuf.clear();
                }
                saveBuf.put((byte) 0x01);
                saveBuf.putInt(size);
                saveBuf.put(data, 0, size);
                saveLock.notify();
            }
        } else if (NotifyType.NOTI_MICOUT_AAC == type) {
            short channel = ByteUtil.bytes2Short(params, 0, true);
            if (mChannel != channel) return;
            synchronized (saveLock) {
                if (saveBuf.remaining() < size) {
                    FlyLog.e("save buffer[%d] is full, clean all buffer!", mChannel);
                    saveBuf.clear();
                }
                saveBuf.put((byte) 0x02);
                saveBuf.putInt(size);
                saveBuf.put(data, 0, size);
                saveLock.notify();
            }
        }
    }
}
