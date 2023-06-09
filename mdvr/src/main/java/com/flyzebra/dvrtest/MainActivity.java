package com.flyzebra.dvrtest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import com.flyzebra.dvrtest.databinding.ActivityMainBinding;
import com.flyzebra.utils.FlyLog;
import com.quectel.qcarapi.stream.QCarCamera;

public class MainActivity extends AppCompatActivity {
    private SurfaceView sv01 = null;
    private SurfaceView sv02 = null;
    private SurfaceView sv03 = null;
    private SurfaceView sv04 = null;
    private QCarCamera qCarCamera = null;

    // Used to load the 'dvrtest' library on application startup.
    static {
        System.loadLibrary("mdvr_zebra");
        System.loadLibrary("mmqcar_qcar_jni");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sv01 = findViewById(R.id.sv01);
        sv02 = findViewById(R.id.sv02);
        sv03 = findViewById(R.id.sv03);
        sv04 = findViewById(R.id.sv04);
        //cameraPowerUp();
        qCarCamera = new QCarCamera(1);
        int ret = qCarCamera.cameraOpen(4, 3);
        if (ret == 0) {
            FlyLog.e("camera open success!");
        } else if (ret > 0) {
            FlyLog.e("camera don't close, ret=%d", ret);
        } else {
            FlyLog.e("camera open failed, ret=%d", ret);
        }

        sv01.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if (qCarCamera != null) {
                    qCarCamera.startPreview(0, holder.getSurface(), 1280, 720, QCarCamera.YUV420_NV21);
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                if (qCarCamera != null) {
                    qCarCamera.stopPreview(0);
                }
            }
        });


        sv02.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if (qCarCamera != null) {
                    qCarCamera.startPreview(1, holder.getSurface(), 1280, 720, QCarCamera.YUV420_NV21);
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                if (qCarCamera != null) {
                    qCarCamera.stopPreview(1);
                }
            }
        });

        sv03.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if (qCarCamera != null) {
                    qCarCamera.startPreview(2, holder.getSurface(), 1280, 720, QCarCamera.YUV420_NV21);
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                if (qCarCamera != null) {
                    qCarCamera.stopPreview(2);
                }
            }
        });

        sv04.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if (qCarCamera != null) {
                    qCarCamera.startPreview(3, holder.getSurface(), 1280, 720, QCarCamera.YUV420_NV21);
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                if (qCarCamera != null) {
                    qCarCamera.stopPreview(3);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        //cameraPowerDown();
        qCarCamera.cameraClose();
        qCarCamera.release();
        super.onDestroy();
    }

    /**
     * A native method that is implemented by the 'dvrtest' native library,
     * which is packaged with this application.
     */
    public native void cameraPowerUp();

    public native void cameraPowerDown();

}