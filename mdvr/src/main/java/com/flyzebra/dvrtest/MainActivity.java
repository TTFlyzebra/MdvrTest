package com.flyzebra.dvrtest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import com.flyzebra.dvrtest.databinding.ActivityMainBinding;
import com.flyzebra.utils.FlyLog;
import com.quectel.qcarapi.cb.IQCarAudioDataCB;
import com.quectel.qcarapi.stream.QCarAudio;
import com.quectel.qcarapi.stream.QCarCamera;
import com.quectel.qcarapi.util.QCarLog;

public class MainActivity extends AppCompatActivity implements IQCarAudioDataCB {

    // Used to load the 'dvrtest' library on application startup.
    static {
        System.loadLibrary("mmqcar_qcar_jni");
    }

    private ActivityMainBinding binding;
    private final int MAX_CAM = 4;
    private final SurfaceView[] mSurfaceViews = new SurfaceView[MAX_CAM];
    private final int[] mSurfaceViewIds = new int[]{R.id.sv01, R.id.sv02, R.id.sv03, R.id.sv04};
    private final Surface[] mSurface = new Surface[MAX_CAM];
    private final boolean[] isPreview = new boolean[MAX_CAM];
    private QCarCamera qCarCamera = null;
    private int camer_open_ret = 0;
    private static final Handler mHander = new Handler(Looper.getMainLooper());

    private final Runnable camerTask = new Runnable() {
        @Override
        public void run() {
            try {
                if (qCarCamera == null) {
                    qCarCamera = new QCarCamera(1);
                }
                camer_open_ret = qCarCamera.cameraOpen(4, 1);
                if (camer_open_ret == 0) {
                    FlyLog.d("camera open success!");
                    for (int i = 0; i < MAX_CAM; i++) {
                        starPreviewCamera(i);
                    }

                    try {
                        if (qCarAudio == null) {
                            qCarAudio = QCarAudio.getInstance();
                        }
                        if (qCarAudio != null) {
                            qCarAudio.configureAudioParam(
                                    QCarAudio.QUEC_SAMPLINGRATE_48,
                                    1,
                                    QCarAudio.QUEC_PCMSAMPLEFORMAT_FIXED_16,
                                    QCarAudio.QUEC_SPEAKER_FRONT_LEFT,
                                    QCarAudio.QUEC_BYTEORDER_LITTLEENDIAN
                            );
                            qCarAudio.registerQCarAudioDataCB(MainActivity.this);
                            qCarAudio.startRecorder();
                        } else {
                            FlyLog.e("Get QCarAudio failed!");
                        }
                    } catch (Exception e) {
                        FlyLog.e(e.toString());
                    }

                } else {
                    FlyLog.e("camera open failed, ret=%d", camer_open_ret);
                    mHander.postDelayed(camerTask, 1000);
                }
            } catch (Exception e) {
                FlyLog.e(e.toString());
                mHander.postDelayed(camerTask, 1000);
            }
        }
    };


    private QCarAudio qCarAudio = null;

    @Override
    public void onAudioChannelStream(int i, byte[] bytes, int i1) {
        FlyLog.e("onAudioChannelStream recv pcm channel=%d, size=%d", i, i1);
    }

    private class MySurfaceCallback implements SurfaceHolder.Callback {
        private int num = 4;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        QCarLog.setTagLogLevel(Log.VERBOSE);

        for (int i = 0; i < MAX_CAM; i++) {
            mSurface[i] = null;
            isPreview[i] = false;
            mSurfaceViews[i] = findViewById(mSurfaceViewIds[i]);
            mSurfaceViews[i].getHolder().addCallback(new MySurfaceCallback(i));
        }
        mHander.post(camerTask);
    }

    @Override
    protected void onDestroy() {
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
            qCarCamera.cameraClose();
        }
        qCarCamera.release();
        super.onDestroy();
        FlyLog.d("MdvrTest exit!");
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
}