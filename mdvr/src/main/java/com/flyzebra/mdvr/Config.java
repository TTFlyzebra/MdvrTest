package com.flyzebra.mdvr;

import android.media.AudioFormat;
import android.media.MediaFormat;

public class Config {
    public static String CAM_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    public static int CAM_WIDTH = 1280;
    public static int CAM_HEIGHT = 720;
    public static int MAX_CAM = 4;
    public static int FRAME_RATE = 25;
    public static int I_FRAME_INTERVAL = 5;
    public static int BIT_RATE = 2048;
    public static int BITRATE_MODE = 1; //BITRATE_MODE_VBR = 1, BITRATE_MODE_CBR = 2;

    public static String MIC_MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;
    public static int MIC_SAMPLE = 48000;
    public static int MIC_CHANNELS = 2;
    public static int MIC_BIT_RATE = 32 * 1024;
    public static int MIC_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static int MIC_CHANNEL = AudioFormat.CHANNEL_IN_STEREO;

    public static String RTMP_URL = "rtmp://192.168.3.8/live";

    public static long MIN_STORE = 4294967296L;//4G
    public static long RECORD_TIME = 300000;//5min

    public static String WIFI_SSID = "\"F-6165\"";
    public static String WIFI_PSWD = "\"12344321\"";

    public static int ADAS_FRAME_RATE = 12;
    public static int DMS_FRAME_RATE = 9;

    public static final String appId= "vdpeYwDGohrtjAyMZdQMwNS3DPQF66";
    public static final String appSecret = "dyqtqEpNyKLUZaMVAZdFhgtjskymaR";
}
