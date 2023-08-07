package com.flyzebra.mdvr.camera;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.NotifyType;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;
import com.quectel.qcarapi.stream.QCarCamera;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraService1080P implements Runnable {
    public static final int CAM_WIDTH = 1920;
    public static final int CAM_HEIGHT = 1080;
    public static final int MAX_CAM = 4;
    private Context mContext;
    private int width;
    private int height;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private QCarCamera qCarCamera1 = null;
//    private QCarCamera qCarCamera2 = null;
    private int camer_open_ret = -1;
    private final AtomicBoolean is_stop = new AtomicBoolean(true);
    private final VideoYuvThread[] yuvThreads = new VideoYuvThread[MAX_CAM];
    private final ByteBuffer[] videoBuffer = new ByteBuffer[MAX_CAM];
    private final CameraEncoder[] cameraEncoders = new CameraEncoder[MAX_CAM];

    public CameraService1080P(Context context) {
        mContext = context;
        for (int i = 0; i < MAX_CAM; i++) {
            cameraEncoders[i] = new CameraEncoder(i, CAM_WIDTH, CAM_HEIGHT);
        }
    }

    public void onCreate() {
        FlyLog.d("YuvService start!");
        this.width = CAM_WIDTH;
        this.height = CAM_HEIGHT;
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
            cameraEncoders[i].onDistory();
        }

        for (int i = 0; i < MAX_CAM; i++) {
            try {
                if (yuvThreads[i] != null) yuvThreads[i].join();
            } catch (InterruptedException e) {
                FlyLog.e(e.toString());
            }
        }

        if (camer_open_ret == 0) {
            for (int i = 0; i < MAX_CAM; i++) {
                if(qCarCamera1!=null)qCarCamera1.stopVideoStream(i);
//                if(qCarCamera2!=null)qCarCamera2.stopVideoStream(i);
            }
            if(qCarCamera1!=null){
                qCarCamera1.cameraClose();
                qCarCamera1.release();
            }
//            if(qCarCamera2!=null) {
//                qCarCamera2.cameraClose();
//                qCarCamera2.release();
//            }

        }
        FlyLog.d("YuvService exit!");
    }

    @Override
    public void run() {
        if (qCarCamera1 == null) {
            qCarCamera1 = new QCarCamera(1);
        }
//        if(qCarCamera2 == null){
//            qCarCamera2 = new QCarCamera(2);
//        }
        camer_open_ret = qCarCamera1.cameraOpen(4, 5);
        if (camer_open_ret != 0) {
            FlyLog.e("QCarCamera open failed, ret=%d", camer_open_ret);
            mHandler.postDelayed(CameraService1080P.this, 1000);
            return;
        }
//        camer_open_ret = qCarCamera2.cameraOpen(4, 4);
//        if (camer_open_ret != 0) {
//            FlyLog.e("QCarCamera2 open failed, ret=%d", camer_open_ret);
//            mHandler.postDelayed(CameraService_1080.this, 1000);
//            return;
//        }
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
            final int size = width * height * 3 / 2;
            //if(channel<2) {
            qCarCamera1.setVideoSize(channel, width, height);
            videoBuffer[channel] = ByteBuffer.wrap(new byte[size]);
            qCarCamera1.setVideoColorFormat(channel, QCarCamera.YUV420_NV12);
            qCarCamera1.startVideoStream(channel);
            //}else {
//            qCarCamera2.setVideoSize(channel, width, height);
//            videoBuffer[channel] = ByteBuffer.wrap(new byte[size]);
//            qCarCamera2.setVideoColorFormat(channel, QCarCamera.YUV420_NV12);
//            qCarCamera2.startVideoStream(channel);
            //}
            while (!is_stop.get()) {
                QCarCamera.FrameInfo info = qCarCamera1.getVideoFrameInfo(channel, videoBuffer[channel]);
//                if (info == null){
//                    //FlyLog.e("qCarCamera1 getVideoFrameInfo %d = null", channel);
//                    info = qCarCamera2.getVideoFrameInfo(channel, videoBuffer[channel]);
//                }
                if (info == null) {
                    //FlyLog.e("qCarCamera2 getVideoFrameInfo %d = null", channel);
                    continue;
                }
                //FlyLog.e("camera=%d ptsSec=%d,ptsUsec=%d,frameID=%d", channel, info.ptsSec, info.ptsUsec, info.frameID);
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
