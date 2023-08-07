package com.flyzebra.mdvr.model;

import com.flyzebra.utils.FlyLog;

import java.io.IOException;
import java.io.RandomAccessFile;

public class ZebraMuxer {
    public static final int VIDEO_HEAD = 1;
    public static final int AUDIO_HEAD = 2;
    public static final int VIDEO_FRAME = 3;
    public static final int AUDIO_FRAME = 4;
    private RandomAccessFile file = null;

    public ZebraMuxer(String path) {
        try {
            file = new RandomAccessFile(path, "rws");
        } catch (Exception e) {
            FlyLog.e(e.toString());
        }
    }

    public void addVideoTrack(byte[] videoHead, int size) {
        if (file != null) {
            try {
                file.writeInt(VIDEO_HEAD);
                file.writeInt(size);
                file.write(videoHead, 0, size);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void addAudioTrack(byte[] audioHead, int size) {
        if (file != null) {
            try {
                file.writeInt(AUDIO_HEAD);
                file.writeInt(size);
                file.write(audioHead, 0, size);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void writeVideoFrame(byte[] data, int size, long pts) {
        if (file != null) {
            try {
                file.writeInt(VIDEO_FRAME);
                file.writeInt(size + 8);
                file.writeLong(pts);
                file.write(data, 0, size);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void writeAudioFrame(byte[] data, int size, long pts) {
        if (file != null) {
            try {
                file.writeInt(AUDIO_FRAME);
                file.writeInt(size + 8);
                file.writeLong(pts);
                file.write(data, 0, size);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void close() {
        if (file != null) {
            try {
                file.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
