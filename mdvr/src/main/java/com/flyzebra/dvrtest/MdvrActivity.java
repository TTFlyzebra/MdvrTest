package com.flyzebra.dvrtest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;
import com.quectel.qcarapi.stream.QCarAudio;
import com.quectel.qcarapi.stream.QCarCamera;
import com.quectel.qcarapi.stream.QCarCamera.FrameInfo;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class MdvrActivity extends AppCompatActivity {

    static {
        System.loadLibrary("mmqcar_qcar_jni");
    }

    private final int MAX_CAM = 4;
    private final SurfaceView[] mSurfaceViews = new SurfaceView[MAX_CAM];
    private final int[] mSurfaceViewIds = new int[]{R.id.sv01, R.id.sv02, R.id.sv03, R.id.sv04};
    private final Surface[] mSurface = new Surface[MAX_CAM];
    private final boolean[] isPreview = new boolean[MAX_CAM];
    private QCarCamera qCarCamera = null;
    private int camer_open_ret = -1;
    private QCarAudio qCarAudio = null;
    private static final Handler mHander = new Handler(Looper.getMainLooper());
    private AtomicBoolean is_stop = new AtomicBoolean(true);
    private Thread mainVideoThread = null;
    private final ByteBuffer[] videoBuffer = new ByteBuffer[MAX_CAM];
    private final Object workLock = new Object();

    private final Runnable camerTask = new Runnable() {
        @Override
        public void run() {
            try {
                if (qCarCamera == null) {
                    qCarCamera = new QCarCamera(1);
                }
                camer_open_ret = qCarCamera.cameraOpen(4, 1);
                if (camer_open_ret != 0) {
                    FlyLog.e("camera open failed, ret=%d", camer_open_ret);
                    mHander.postDelayed(camerTask, 1000);
                    return;
                }
                FlyLog.d("camera open success!");

                for (int i = 0; i < MAX_CAM; i++) {
                    qCarCamera.setVideoSize(i, Config.VIDEO_WIDTH, Config.VIDEO_HEIGHT);
                }
                qCarCamera.startVideoStream(0);

                if (qCarAudio == null) {
                    qCarAudio = QCarAudio.getInstance();
                }
                qCarAudio.setMute(false);
                int ret = qCarAudio.configureAudioParam(QCarAudio.QUEC_SAMPLINGRATE_48,
                        2,
                        QCarAudio.QUEC_PCMSAMPLEFORMAT_FIXED_16,
                        QCarAudio.QUEC_SPEAKER_FRONT_LEFT | QCarAudio.QUEC_SPEAKER_FRONT_RIGHT,
                        QCarAudio.QUEC_BYTEORDER_LITTLEENDIAN);
                FlyLog.e("configureAudioParam ret=%d", ret);
                qCarAudio.setSplitChannelAndByteNums(4, 1);
                qCarAudio.registerQCarAudioDataCB((channelNum, pBuf, nLen) -> {
                    FlyLog.e("channel=%d:%s", channelNum, ByteUtil.bytes2String(pBuf, 48));
                });
                qCarAudio.startAudioStream(0);
                qCarAudio.startAudioStream(1);
                qCarAudio.startAudioStream(2);
                qCarAudio.startAudioStream(3);
                qCarAudio.startRecorder();

                for (int i = 0; i < MAX_CAM; i++) {
                    starPreviewCamera(i);
                }

                synchronized (workLock) {
                    workLock.notifyAll();
                }
            } catch (Exception e) {
                FlyLog.e(e.toString());
                mHander.postDelayed(camerTask, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        for (int i = 0; i < MAX_CAM; i++) {
            mSurface[i] = null;
            isPreview[i] = false;
            mSurfaceViews[i] = findViewById(mSurfaceViewIds[i]);
            mSurfaceViews[i].getHolder().addCallback(new MySurfaceCallback(i));
        }
        mHander.post(camerTask);

        for (int i = 0; i < MAX_CAM; i++) {
            videoBuffer[i] = ByteBuffer.allocateDirect(Config.VIDEO_WIDTH * Config.VIDEO_HEIGHT * 3 / 2);
        }

        mainVideoThread = new Thread(() -> {
            synchronized (workLock) {
                try {
                    if (camer_open_ret != 0 && !is_stop.get()) {
                        workLock.wait();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            while (!is_stop.get()) {
                FrameInfo info = qCarCamera.getVideoFrameInfo(0, videoBuffer[0]);
                //FlyLog.e("camera=%d ptsSec=%d,ptsUsec=%d,frameID=%d", 0, info.ptsSec, info.ptsUsec, info.frameID);
            }
        }, "mdvr-cam0");
        mainVideoThread.start();
        is_stop.set(false);
    }


    private void starPreviewCamera(int num) {
        if (qCarCamera != null && mSurface[num] != null && !isPreview[num]) {
            isPreview[num] = true;
            qCarCamera.startPreview(num, mSurface[num], 1280, 720, QCarCamera.YUV420_NV21);
        }
    }

    private void stopPreviewCamera(int num) {
        if (qCarCamera != null) {
            isPreview[num] = false;
            qCarCamera.stopPreview(num);
        }
    }

    @Override
    protected void onDestroy() {
        try {
            is_stop.set(true);
            synchronized (workLock) {
                workLock.notifyAll();
            }
            mainVideoThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
            if (qCarAudio != null) {
                qCarAudio.registerQCarAudioDataCB(null);
                qCarAudio.stopRecorder();
            }
        } catch (Exception e) {
            FlyLog.e(e.toString());
        }
        mHander.removeCallbacksAndMessages(null);
        if (camer_open_ret == 0) {
            for (int i = 0; i < MAX_CAM; i++) {
                qCarCamera.stopVideoStream(i);
            }
            qCarCamera.cameraClose();
        }
        qCarCamera.release();

        super.onDestroy();
        FlyLog.d("MdvrTest exit!");
    }

    private class MySurfaceCallback implements SurfaceHolder.Callback {
        private int num = 0;

        public MySurfaceCallback(int num) {
            this.num = num;
        }

        @Override
        public void surfaceCreated(@NonNull SurfaceHolder holder) {
            mSurface[num] = holder.getSurface();
            starPreviewCamera(num);
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            mSurface[num] = null;
            stopPreviewCamera(num);
        }
    }
}