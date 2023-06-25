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

import com.flyzebra.dvrtest.opengl.GlVideoView;
import com.flyzebra.utils.FlyLog;
import com.quectel.qcarapi.stream.QCarCamera;
import com.quectel.qcarapi.stream.QCarCamera.FrameInfo;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class MdvrActivity extends AppCompatActivity {

    static {
        System.loadLibrary("mmqcar_qcar_jni");
    }

    private final int MAX_CAM = 4;
    private final GlVideoView[] mGlVideoViews = new GlVideoView[MAX_CAM];
    private final int[] mGlVideoViewIds = new int[]{R.id.sv01, R.id.sv02, R.id.sv03, R.id.sv04};
    private final Surface[] mSurface = new Surface[MAX_CAM];
    private final boolean[] isPreview = new boolean[MAX_CAM];
    private QCarCamera qCarCamera = null;
    private int camer_open_ret = -1;
    //private QCarAudio qCarAudio = null;
    private static final Handler mHander = new Handler(Looper.getMainLooper());
    private AtomicBoolean is_stop = new AtomicBoolean(true);
    private VideoYuvThread[] yuvThreads = new VideoYuvThread[4];
    private final ByteBuffer[] videoBuffer = new ByteBuffer[MAX_CAM];

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
                    yuvThreads[i] = new VideoYuvThread(i);
                    yuvThreads[i].start();
                }

                //if (qCarAudio == null) {
                //    qCarAudio = QCarAudio.getInstance();
                //}
                //qCarAudio.setMute(false);
                //int ret = qCarAudio.configureAudioParam(QCarAudio.QUEC_SAMPLINGRATE_48,
                //        2,
                //        QCarAudio.QUEC_PCMSAMPLEFORMAT_FIXED_16,
                //        QCarAudio.QUEC_SPEAKER_FRONT_LEFT | QCarAudio.QUEC_SPEAKER_FRONT_RIGHT,
                //        QCarAudio.QUEC_BYTEORDER_LITTLEENDIAN);
                //FlyLog.e("configureAudioParam ret=%d", ret);
                //qCarAudio.setSplitChannelAndByteNums(4, 1);
                //qCarAudio.registerQCarAudioDataCB((channelNum, pBuf, nLen) -> {
                //    FlyLog.e("channel=%d:%s", channelNum, ByteUtil.bytes2String(pBuf, 48));
                //});
                //qCarAudio.startAudioStream(0);
                //qCarAudio.startAudioStream(1);
                //qCarAudio.startAudioStream(2);
                //qCarAudio.startAudioStream(3);
                //qCarAudio.startRecorder();

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
            mGlVideoViews[i] = findViewById(mGlVideoViewIds[i]);
        }
        mHander.post(camerTask);
        is_stop.set(false);
    }

    @Override
    protected void onDestroy() {
        is_stop.set(true);
        mHander.removeCallbacksAndMessages(null);

        for (int i = 0; i < MAX_CAM; i++) {
            try {
                if (yuvThreads != null) yuvThreads[i].join();
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

        //try {
        //    if (qCarAudio != null) {
        //        qCarAudio.registerQCarAudioDataCB(null);
        //        qCarAudio.stopRecorder();
        //    }
        //} catch (Exception e) {
        //    FlyLog.e(e.toString());
        //}

        super.onDestroy();
        FlyLog.d("MdvrTest exit!");
    }

    private class VideoYuvThread extends Thread implements Runnable {
        private final int number;

        public VideoYuvThread(int number) {
            this.number = number;
            setName("camera-" + number);
        }

        @Override
        public void run() {
            qCarCamera.setVideoSize(number, Config.VIDEO_WIDTH, Config.VIDEO_HEIGHT);
            videoBuffer[number] = ByteBuffer.wrap(new byte[Config.VIDEO_WIDTH * Config.VIDEO_HEIGHT * 3 / 2]);
            qCarCamera.setVideoColorFormat(number, QCarCamera.YUV420_NV21);
            qCarCamera.startVideoStream(number);
            while (!is_stop.get()) {
                FrameInfo info = qCarCamera.getVideoFrameInfo(number, videoBuffer[number]);
                //FlyLog.e("camera=%d ptsSec=%d,ptsUsec=%d,frameID=%d", number, info.ptsSec, info.ptsUsec, info.frameID);
                mGlVideoViews[number].pushNv21data(videoBuffer[number].array(), Config.VIDEO_WIDTH, Config.VIDEO_HEIGHT);
            }
        }
    }
}