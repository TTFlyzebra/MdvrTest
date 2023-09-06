package com.flyzebra.core.media;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;

import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class ZebraMuxer {
    public static final int VIDEO_HEAD = 1;
    public static final int AUDIO_HEAD = 2;
    public static final int VIDEO_FRAME = 3;
    public static final int AUDIO_FRAME = 4;
    private String fileName;
    private RandomAccessFile file = null;
    private final Object saveLock = new Object();
    private final ByteBuffer saveBuf = ByteBuffer.allocateDirect(10 * 1024 * 1024);
    private final AtomicBoolean is_stop = new AtomicBoolean(true);
    private Thread workThread = null;
    private static final int MIN_WRITE_SIZE = 64 * 1024;
    byte[] write_data = new byte[MIN_WRITE_SIZE];

    private static final HandlerThread fileThread = new HandlerThread("File_Rename");

    static {
        fileThread.start();
    }

    private static final Handler tHandler = new Handler(fileThread.getLooper());

    public ZebraMuxer(String path) {
        try {
            this.fileName = path;
            file = new RandomAccessFile(path + ".tmp", "rws");
        } catch (Exception e) {
            FlyLog.e(e.toString());
        }

        is_stop.set(false);
        workThread = new Thread(() -> {
            while (!is_stop.get()) {
                synchronized (saveLock) {
                    while (!is_stop.get() && saveBuf.position() < MIN_WRITE_SIZE) {
                        try {
                            saveLock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (is_stop.get()) break;
                    saveBuf.flip();
                    saveBuf.get(write_data, 0, MIN_WRITE_SIZE);
                    saveBuf.compact();
                }
                long stime = SystemClock.uptimeMillis();
                try {
                    file.write(write_data, 0, MIN_WRITE_SIZE);
                } catch (IOException e) {
                    FlyLog.e(e.toString());
                }
                long utime = SystemClock.uptimeMillis() - stime;
            }
            synchronized (saveLock) {
                saveBuf.flip();
                try {
                    file.write(saveBuf.array(), 0, saveBuf.limit());
                } catch (IOException e) {
                    FlyLog.e(e.toString());
                }
            }
        }, "write_file");
        workThread.start();
    }

    public void addVideoTrack(byte[] videoHead, int size) {
        if (!is_stop.get()) {
            byte[] data = new byte[size + 8];
            ByteUtil.intToBytes(size, data, 0, true);
            ByteUtil.intToBytes(VIDEO_HEAD, data, 4, true);
            System.arraycopy(videoHead, 0, data, 8, size);
            synchronized (saveLock) {
                if (saveBuf.remaining() < size + 8) {
                    FlyLog.e("save file buffer is full, clean all buffer!");
                    saveBuf.clear();
                }
                saveBuf.put(data, 0, size + 8);
                saveLock.notify();
            }
        }
    }

    public void addAudioTrack(byte[] audioHead, int size) {
        if (!is_stop.get()) {
            byte[] data = new byte[size + 8];
            ByteUtil.intToBytes(size, data, 0, true);
            ByteUtil.intToBytes(AUDIO_HEAD, data, 4, true);
            System.arraycopy(audioHead, 0, data, 8, size);
            synchronized (saveLock) {
                if (saveBuf.remaining() < size + 8) {
                    FlyLog.e("save file buffer is full, clean all buffer!");
                    saveBuf.clear();
                }
                saveBuf.put(data, 0, size + 8);
                saveLock.notify();
            }
        }
    }

    public void writeVideoFrame(byte[] data, int offset, int size) {
        if (!is_stop.get()) {
            synchronized (saveLock) {
                if (saveBuf.remaining() < size - offset) {
                    FlyLog.e("save file buffer is full, clean all buffer!");
                    saveBuf.clear();
                }
                saveBuf.put(data, offset, size);
                saveLock.notify();
            }
        }
    }

    public void writeAudioFrame(byte[] data, int offset, int size) {
        if (!is_stop.get()) {
            synchronized (saveLock) {
                if (saveBuf.remaining() < size - offset) {
                    FlyLog.e("save file buffer is full, clean all buffer!");
                    saveBuf.clear();
                }
                saveBuf.put(data, offset, size);
                saveLock.notify();
            }
        }
    }

    public void close(boolean flag) {
        tHandler.post(() -> {
            is_stop.set(true);
            synchronized (saveLock) {
                saveLock.notifyAll();
            }
            if (workThread != null) {
                try {
                    workThread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                workThread = null;
            }
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                    FlyLog.e(e.toString());
                }
                if (flag) {
                    File tmpFile = new File(fileName + ".tmp");
                    for (int i = 0; i < 3; i++) {
                        if (tmpFile.renameTo(new File(fileName + ".mp4"))) {
                            FlyLog.d("rename file name to " + fileName + ".mp4");
                            break;
                        } else {
                            //TODO:
                            FlyLog.e("rename file name to " + fileName + ".mp4 failed!");
                        }
                    }
                }
            }
        });
    }
}
