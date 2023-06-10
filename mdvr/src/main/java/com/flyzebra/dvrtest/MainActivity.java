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

import com.flyzebra.dvrtest.databinding.ActivityMainBinding;
import com.flyzebra.utils.FlyLog;
import com.quectel.qcarapi.stream.QCarCamera;

public class MainActivity extends AppCompatActivity {

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
    private static final Handler mHander = new Handler(Looper.getMainLooper());

    private final Runnable camerTask = new Runnable() {
        @Override
        public void run() {
            qCarCamera = GUtilMain.getQCamera(1);
            int ret = qCarCamera.cameraOpen(4, 1);
            if (ret == 0) {
                FlyLog.d("camera open success!");
                for (int i = 0; i < MAX_CAM; i++) {
                    starPreviewCamera(i);
                }
            } else {
                FlyLog.e("camera open failed, ret=%d", ret);
                mHander.postDelayed(camerTask, 1000);
            }
        }
    };

    private class MySurfaceCallback implements SurfaceHolder.Callback {
        private int num;

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
        mHander.removeCallbacksAndMessages(null);
        qCarCamera.cameraClose();
        super.onDestroy();
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