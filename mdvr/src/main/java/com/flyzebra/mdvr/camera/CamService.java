package com.flyzebra.mdvr.camera;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.NotifyType;
import com.flyzebra.mdvr.Config;
import com.flyzebra.mdvr.Global;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.DateUtil;
import com.flyzebra.utils.FlyLog;
import com.quectel.qcarapi.osd.QCarOsd;
import com.quectel.qcarapi.stream.QCarCamera;
import com.quectel.qcarapi.util.QCarLog;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class CamService {
    private Context mContext;
    private int width;
    private int height;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private QCarCamera qCarCamera = null;
    private int camer_open_ret = -1;
    private final AtomicBoolean is_stop = new AtomicBoolean(true);
    private final VideoYuvThread[] yuvThreads = new VideoYuvThread[Config.MAX_CAM];
    private final ByteBuffer[] videoBuffer = new ByteBuffer[Config.MAX_CAM];

    protected final Runnable openCameraTasker = () -> {
        if (qCarCamera == null) {
            qCarCamera = new QCarCamera(1);
        }
        if (Config.CAM_WIDTH == 1920) {
            camer_open_ret = qCarCamera.cameraOpen(4, 3);
        } else {
            camer_open_ret = qCarCamera.cameraOpen(4, 1);
        }
        if (camer_open_ret != 0) {
            FlyLog.e("QCarCamera open failed, ret=%d", camer_open_ret);
            mHandler.postDelayed(this.openCameraTasker, 1000);
            return;
        }

        Global.qCarCameras.put(1, qCarCamera);
        FlyLog.d("QCarCamera open success!");

        //添加水印
        QCarOsd osd = new QCarOsd();
        String font_path = "/system/fonts/RobotoCondensed-Light.ttf";
        osd.initOsd(font_path.getBytes(), 4);
        osd.setOsdColor(255, 0, 0);
        qCarCamera.setMainOsd(osd);
        for (int i = 0; i < Config.MAX_CAM; i++) {
            String text = "CHANNEL-" + (i + 1) + DateUtil.getCurrentDate(" yyyy-MM-dd HH:mm:ss");
            osd.setOsd(i, text.getBytes(), -1, 800, 40);
        }
        new Thread(() -> {
            while (!is_stop.get()) {
                for (int i = 0; i < Config.MAX_CAM; i++) {
                    String text = "CHANNEL-" + (i + 1) + DateUtil.getCurrentDate(" yyyy-MM-dd HH:mm:ss");
                    osd.setOsd(i, text.getBytes(), i, 800, 40);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        //添加水印结束

        for (int i = 0; i < Config.MAX_CAM; i++) {
            yuvThreads[i] = new VideoYuvThread(i);
            yuvThreads[i].start();
        }
    };

    public CamService(Context context) {
        mContext = context;
    }

    public void start() {
        FlyLog.d("CamService start!");
        this.width = Config.CAM_WIDTH;
        this.height = Config.CAM_HEIGHT;
        is_stop.set(false);
        QCarLog.setTagLogLevel(Log.ASSERT);
        mHandler.post(openCameraTasker);
    }

    public void stop() {
        is_stop.set(true);
        mHandler.removeCallbacksAndMessages(null);

        for (int i = 0; i < Config.MAX_CAM; i++) {
            try {
                if (yuvThreads[i] != null) yuvThreads[i].join();
            } catch (InterruptedException e) {
                FlyLog.e(e.toString());
            }
        }

        if (camer_open_ret == 0 && qCarCamera != null) {
            qCarCamera.cameraClose();
            qCarCamera.release();
        }
        FlyLog.d("CamService exit!");
    }

    private class VideoYuvThread extends Thread implements Runnable {
        private final int channel;

        public VideoYuvThread(int number) {
            this.channel = number;
            setName("camera-" + number);
        }

        @Override
        public void run() {
            qCarCamera.setVideoSize(channel, width, height);
            final int size = width * height * 3 / 2;
            videoBuffer[channel] = ByteBuffer.allocateDirect(size);
            qCarCamera.setVideoColorFormat(channel, QCarCamera.YUV420_NV12);
            qCarCamera.startVideoStream(channel);
            long frame_time = 1000L / Config.FRAME_RATE;
            long last_time = SystemClock.uptimeMillis() - frame_time;
            while (!is_stop.get()) {
                QCarCamera.FrameInfo info = qCarCamera.getVideoFrameInfo(channel, videoBuffer[channel]);
                if (info != null) {
                    //FlyLog.e("camera=%d ptsSec=%d,ptsUsec=%d,frameID=%d", channel, info.ptsSec, info.ptsUsec, info.frameID);
                    long ptsUsec = info.ptsSec * 1000000 + info.ptsUsec;
                    //long ptsUsec = System.nanoTime() / 1000;
                    byte[] params = new byte[14];
                    ByteUtil.shortToBytes((short) channel, params, 0, true);
                    ByteUtil.shortToBytes((short) width, params, 2, true);
                    ByteUtil.shortToBytes((short) height, params, 4, true);
                    ByteUtil.longToBytes(ptsUsec, params, 6, true);
                    Notify.get().handledata(NotifyType.NOTI_CAMOUT_YUV, videoBuffer[channel].array(), size, params, params.length);
                } else {
                    FlyLog.e("Camera getVideoFrameInfo return null!");
                }
                //control fps
                long current_time = SystemClock.uptimeMillis();
                long sleep_time = frame_time - (current_time - last_time);
                if (sleep_time > 0 && sleep_time < frame_time) {
                    try {
                        Thread.sleep(sleep_time);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                last_time = SystemClock.uptimeMillis();
            }
            qCarCamera.stopVideoStream(channel);
        }
    }
}
