package com.flyzebra.mdvr;

import android.media.AudioFormat;
import android.media.MediaFormat;

public class Config {
    public static final String CAM_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_HEVC;
    public static final int CAM_WIDTH = 1920;
    public static final int CAM_HEIGHT = 1080;
    public static final int CAM_BIT_RATE = 2000000;
    public static final int CSI_NUM = 1;
    public static final int MAX_CAM = 4;
    public static final int SET_NUM = 1;

    public static final int MAX_FRAME_SIZE = CAM_WIDTH * CAM_HEIGHT * 3 / 2;

    public static final String MIC_MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;
    public static final int MIC_SAMPLE = 48000;
    public static final int MIC_CHANNELS = 2;
    public static final int MIC_BIT_RATE = 32 * 1024;
    public static final int MIC_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int MIC_CHANNEL = AudioFormat.CHANNEL_IN_STEREO;

    public static final String RTMP_URL = "rtmp://192.168.3.8/live";
}
