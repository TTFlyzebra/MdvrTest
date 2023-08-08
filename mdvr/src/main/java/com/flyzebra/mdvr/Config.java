package com.flyzebra.mdvr;

import android.media.AudioFormat;
import android.media.MediaFormat;

public class Config {
    public static final String CAM_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    public static final int MAX_CAM = 4;
    public static final int FRAME_RATE = 16;
    public static final int I_FRAME_INTERVAL = 1;
    public static final int BIT_RATE = 2000000;

    public static final String MIC_MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;
    public static final int MIC_SAMPLE = 48000;
    public static final int MIC_CHANNELS = 2;
    public static final int MIC_BIT_RATE = 32 * 1024;
    public static final int MIC_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int MIC_CHANNEL = AudioFormat.CHANNEL_IN_STEREO;

    public static final String RTMP_URL = "rtmp://192.168.3.8/live";

    public static final long MIN_STORE = 4294967296L;//4G
    public static final long RECORD_TIME = 300000;//5min

    public static final String WIFI_SSID = "\"F-6165\"";
    public static final String WIFI_PSWD = "\"12344321\"";
}
