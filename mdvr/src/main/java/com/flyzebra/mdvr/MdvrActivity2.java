package com.flyzebra.mdvr;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.flyzebra.utils.FlyLog;
import com.flyzebra.utils.PropUtil;
import com.flyzebra.utils.ShellUtil;
import com.quectel.qcarapi.stream.QCarCamera;

public class MdvrActivity2 extends AppCompatActivity {
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private final SurfaceView[] mSurfaceViews = new SurfaceView[Config.MAX_CAM];
    private final int[] mSurfaceViewIds = new int[]{R.id.sv01, R.id.sv02, R.id.sv03, R.id.sv04};
    private final Surface[] mSurface = new Surface[Config.MAX_CAM];
    private final boolean[] isPreview = new boolean[Config.MAX_CAM];
    //private MyRecevier recevier = new MyRecevier();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FlyLog.d("MdvrActiviy start!");

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main2);

        PropUtil.set("ctl.stop", "MonitorHobotApk");
        ShellUtil.exec("am force-stop com.hobot.sample.app");

        startService(new Intent(this, MdvrService.class));

        for (int i = 0; i < Config.MAX_CAM; i++) {
            mSurface[i] = null;
            isPreview[i] = false;
            mSurfaceViews[i] = findViewById(mSurfaceViewIds[i]);
            mSurfaceViews[i].getHolder().addCallback(new MySurfaceCallback(i));
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.hardware.usb.action.USB_STATE");
        //registerReceiver(recevier, intentFilter);
        mHandler.post(cameraTasker);
    }

    @Override
    protected void onResume() {
        super.onResume();
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        for (int i = 0; i < Config.MAX_CAM; i++) {
            mSurfaceViews[i].getHolder().removeCallback(new MySurfaceCallback(i));
        }
        //unregisterReceiver(recevier);
        stopService(new Intent(this, MdvrService.class));
        FlyLog.d("MdvrActiviy exit!");
    }


    protected final Runnable cameraTasker = () -> {
        if (Global.qCarCameras.get(1) != null) {
            for (int i = 0; i < Config.MAX_CAM; i++) {
                starPreviewCamera(i);
            }
        } else {
            mHandler.postDelayed(MdvrActivity2.this.cameraTasker, 1000);
        }
    };

    private void starPreviewCamera(int num) {
        if (Global.qCarCameras.get(1) != null && mSurface[num] != null && !isPreview[num]) {
            isPreview[num] = true;
            Global.qCarCameras.get(1).startPreview(num, mSurface[num], 1280, 720, QCarCamera.YUV420_NV21);
        }
    }

    private void stopPreviewCamera(int num) {
        if (Global.qCarCameras.get(1) != null) {
            isPreview[num] = false;
            Global.qCarCameras.get(1).stopPreview(num);
        }
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
}