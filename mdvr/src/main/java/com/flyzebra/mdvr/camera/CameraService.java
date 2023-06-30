package com.flyzebra.mdvr.camera;

import static com.flyzebra.mdvr.Config.MAX_CAM;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.NotifyType;
import com.flyzebra.mdvr.Config;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;
import com.quectel.qcarapi.stream.QCarCamera;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraService implements Runnable {
    private Context mContext;
    private int width;
    private int height;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private QCarCamera qCarCamera = null;
    private int camer_open_ret = -1;
    private final AtomicBoolean is_stop = new AtomicBoolean(true);
    private final VideoYuvThread[] yuvThreads = new VideoYuvThread[4];
    private final ByteBuffer[] videoBuffer = new ByteBuffer[MAX_CAM];
    private final CameraEncoder[] cameraEncoders = new CameraEncoder[MAX_CAM];

    public CameraService(Context context) {
        mContext = context;
        for (int i = 0; i < MAX_CAM; i++) {
            cameraEncoders[i] = new CameraEncoder(i);
        }
    }

    public void onCreate() {
        FlyLog.d("YuvService start!");
        this.width = Config.CAM_WIDTH;
        this.height = Config.CAM_HEIGHT;
        is_stop.set(false);
        for (int i = 0; i < MAX_CAM; i++) {
            cameraEncoders[i].onCreate();
        }
        mHandler.post(this);
    }

    public void onDerstory() {
        is_stop.set(true);
        mHandler.removeCallbacksAndMessages(null);
        for (int i = 0; i < MAX_CAM; i++) {
            try {
                if (yuvThreads[i] != null) yuvThreads[i].join();
            } catch (InterruptedException e) {
                FlyLog.e(e.toString());
            }
        }

        if (camer_open_ret == 0 && qCarCamera != null) {
            for (int i = 0; i < MAX_CAM; i++) {
                qCarCamera.stopVideoStream(i);
            }
            qCarCamera.cameraClose();
            qCarCamera.release();
        }
        for (int i = 0; i < MAX_CAM; i++) {
            cameraEncoders[i].onDistory();
        }
        FlyLog.d("YuvService exit!");
    }

    @Override
    public void run() {
        if (qCarCamera == null) {
            qCarCamera = new QCarCamera(1);
        }
        camer_open_ret = qCarCamera.cameraOpen(4, 1);
        if (camer_open_ret != 0) {
            FlyLog.e("QCarCamera open failed, ret=%d", camer_open_ret);
            mHandler.postDelayed(CameraService.this, 1000);
            return;
        }
        FlyLog.d("QCarCamera open success!");
        for (int i = 0; i < MAX_CAM; i++) {
            yuvThreads[i] = new VideoYuvThread(i);
            yuvThreads[i].start();
        }
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
            videoBuffer[channel] = ByteBuffer.wrap(new byte[size]);
            qCarCamera.setVideoColorFormat(channel, QCarCamera.YUV420_NV12);
            qCarCamera.startVideoStream(channel);
            while (!is_stop.get()) {
                QCarCamera.FrameInfo info = qCarCamera.getVideoFrameInfo(channel, videoBuffer[channel]);
                //FlyLog.e("camera=%d ptsSec=%d,ptsUsec=%d,frameID=%d", number, info.ptsSec, info.ptsUsec, info.frameID);
                long ptsUsec = info.ptsSec * 1000000 + info.ptsUsec;
                //long ptsUsec = System.nanoTime() / 1000;
                byte[] params = new byte[14];
                ByteUtil.shortToBytes((short) channel, params, 0, true);
                ByteUtil.shortToBytes((short) width, params, 2, true);
                ByteUtil.shortToBytes((short) height, params, 4, true);
                ByteUtil.longToBytes(ptsUsec, params, 6, true);
                Notify.get().handledata(NotifyType.NOTI_CAMOUT_YUV, videoBuffer[channel].array(), size, params);
            }
        }
    }
}