/*
 * Copyright (c) 2021, Quectel Wireless Solutions Co., Ltd. All rights reserved.
 * Quectel Wireless Solutions Proprietary and Confidential.
 */
package com.flyzebra.dvrtest;

import android.content.Context;
import android.media.MediaMuxer;
import android.util.Log;
import com.quectel.qcarapi.stream.QCarCamera;
import com.quectel.qcarapi.util.QCarLog;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GUtilMain {

    // Log tag
    private static String TAG = "GUtil";

    // Debug switch
    private static boolean GLOBAL_DEBUG = false;

    // Define video bitrate
    public static int BITRATE_4M = 4 * 1024 * 1024;
    public static int BITRATE_2M = 2 * 1024 * 1024;
    public static int BITRATE_1M = 1024 * 1024;
    public static int BITRATE_512K = 1024 * 1024;
    //private static int BITRATE_DEFAULT = 1024 * 1024;

    private static Context qContext = null;

    // parameters for video recoding
    private static Map<Integer, QCarCamera> qCarCameraMap = new ConcurrentHashMap<Integer, QCarCamera>();
    public static int MEDIA_OUTPUT_FORMAT_TS = 2;

    public static Context getqContext() {
        return qContext;
    }

    public static void setqContext(Context qContext) {
        GUtilMain.qContext = qContext;
    }

    // get the Threshold for segmenting video
    public static int getSegmentSizeWithPosition(int position) {
        if (position == 0) {
            return 50 * 1024 * 1024;
        } else if (position == 1) {
            return 100 * 1024 * 1024;
        } else if (position == 2) {
            return 200 * 1024 * 1024;
        } else if (position == 3) {
            return 1*60*1000; // time : 1min = 60s = 60000ms
        }else if (position == 4) {
            return 3*60*1000;
        }else if (position == 5) {
            return 5*60*1000;
        }else {
            return 50 * 1024 * 1024;
        }
    }

    // get the type for segmenting video
    public static int getSegmentFlag(int position) {
        if (0 == position || 1 == position || 2 == position) {
            return 0;  // 1 means Segment recorded video by Size
        } else if (3 == position || 4 == position || 5 == position) {
            return 1; // 2 means Segment recorded video by time
        } else{
            return 0; // default is 1
        }
    }

    // get the mine type for video codec
    public static String getMinetypeWithCodecPosition(int position) {
        if (position == 0) {
            return "video/avc";
        } else if (position == 1) {
            return "video/hevc";
        }

        return "video/hevc";
    }

    // get output format for video
    public static int getOutputFormatWidthContainerPosition(int position) {
        int ret = 0;
        if (position == 0) {
            return MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4;  // mp4
        } else if (position == 1) {
            String version = android.os.Build.VERSION.RELEASE;
            switch (version) {
                case "7":
                    ret = 2;
                    break;
                case "9":
                    ret = 4;
                    break;
                case "10":
                case "11":
                    ret = 5;
                    break;
                default:
                    Log.e(TAG,"illegal android version:" + version);
                    break;
            }
            return ret; //Android7 该值是2  Android9该值是4 一定要注意
        }

        return MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4; // default is mp4
    }

    public static String removeTempInPath(String path) {
        return path.replace("/temp", "");
    }

    public static synchronized QCarCamera getQCamera(int csiphy_num) {
        if (csiphy_num >= 3) {
            QCarLog.i(QCarLog.LOG_MODULE_APP, TAG, " csiphy_num need small than 3");
            return null;
        }

        QCarCamera qCarCamera = qCarCameraMap.get(csiphy_num);
        if (qCarCamera == null) {
            qCarCamera = new QCarCamera(csiphy_num);
            qCarCameraMap.put(csiphy_num, qCarCamera);
        }

        return qCarCamera;
    }

    public static synchronized int removeQCamera(int csiphy_num) {
        if (csiphy_num >= 3 || csiphy_num < 0) {
            QCarLog.i(QCarLog.LOG_MODULE_APP, TAG, " csiphy_num need larger than 0 and small than 3");
            return -1;
        }

        QCarCamera qCarCamera = qCarCameraMap.get(csiphy_num);
        if (qCarCamera != null) {
            qCarCameraMap.remove(csiphy_num, qCarCamera);
        }

        return 0;
    }
}
