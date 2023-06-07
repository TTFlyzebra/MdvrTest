package com.flyzebra.dvrtest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import com.flyzebra.dvrtest.databinding.ActivityMainBinding;
import com.flyzebra.utils.FlyLog;
import com.quectel.qcarapi.stream.QCarCamera;

public class MainActivity extends AppCompatActivity {

    private SurfaceView sv01;
    private QCarCamera qCarCamera;

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

        cameraPowerUp();

        //qCarCamera = new QCarCamera(1);
        //int ret = qCarCamera.cameraOpen(1, 2);
        //if (ret == 0) {
        //    qCarCamera.startPreview(0, sv01.getHolder().getSurface(), 720, 1280, QCarCamera.YUV420_NV21);
        //} else if (ret > 0) {
        //    FlyLog.e("camera don't close, ret=%d", ret);
        //} else {
        //    FlyLog.e("camera open failed, ret=%d", ret);
        //}
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //qCarCamera.stopPreview(0);
        //qCarCamera.cameraClose();

        cameraPowerDown();
    }

    /**
     * A native method that is implemented by the 'dvrtest' native library,
     * which is packaged with this application.
     */
    public native void cameraPowerUp();

    public native void cameraPowerDown();
}