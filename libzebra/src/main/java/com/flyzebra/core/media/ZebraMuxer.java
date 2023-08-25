package com.flyzebra.core.media;

import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ZebraMuxer {
    public static final int VIDEO_HEAD = 1;
    public static final int AUDIO_HEAD = 2;
    public static final int VIDEO_FRAME = 3;
    public static final int AUDIO_FRAME = 4;
    private String fileName;
    private RandomAccessFile file = null;

    private Executor executor = Executors.newFixedThreadPool(1);

    public ZebraMuxer(String path) {
        try {
            this.fileName = path;
            file = new RandomAccessFile(path + ".tmp", "rws");
        } catch (Exception e) {
            FlyLog.e(e.toString());
        }
    }

    public void addVideoTrack(byte[] videoHead, int size) {
        if (file != null) {
            try {
                byte[] data = new byte[size + 8];
                ByteUtil.intToBytes(size, data, 0, true);
                ByteUtil.intToBytes(VIDEO_HEAD, data, 4, true);
                System.arraycopy(videoHead, 0, data, 8, size);
                file.write(data, 0, size + 8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void addAudioTrack(byte[] audioHead, int size) {
        if (file != null) {
            try {
                byte[] data = new byte[size + 8];
                ByteUtil.intToBytes(size, data, 0, true);
                ByteUtil.intToBytes(AUDIO_HEAD, data, 4, true);
                System.arraycopy(audioHead, 0, data, 8, size);
                file.write(data, 0, size + 8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void writeVideoFrame(byte[] data, int offset, int size) {
        if (file != null) {
            try {
                file.write(data, offset, size);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void writeAudioFrame(byte[] data, int offset, int size) {
        if (file != null) {
            try {
                file.write(data, offset, size);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void close(boolean flag) {
        try {
            if (file != null) {
                file.close();
                if (flag) {
                    executor.execute(() -> {
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
                    });

                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
