package com.flyzebra.mdvr;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.flyzebra.core.notify.INotify;
import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.NotifyType;
import com.flyzebra.mdvr.camera.CameraService;
import com.flyzebra.mdvr.mic.MicService;
import com.flyzebra.mdvr.rtmp.RtmpService;
import com.flyzebra.mdvr.view.GlVideoView;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;
import com.flyzebra.utils.PropUtil;
import com.flyzebra.utils.ShellUtil;

public class MdvrActivity extends AppCompatActivity implements INotify {
    static {
        System.loadLibrary("mmqcar_qcar_jni");
    }

    private final GlVideoView[] mGlVideoViews = new GlVideoView[Config.MAX_CAM];
    private final int[] mGlVideoViewIds = new int[]{R.id.sv01, R.id.sv02, R.id.sv03, R.id.sv04};
    //private MyRecevier recevier = new MyRecevier();

    private final RtmpService rtmpService = new RtmpService(this);
    //private final CameraService_1080 cameraService = new CameraService_1080(this);
    private final CameraService cameraService = new CameraService(this);
    private final MicService micService = new MicService(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FlyLog.d("MdvrActiviy start!");

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        PropUtil.set("ctl.stop", "MonitorHobotApk");
        ShellUtil.exec("am force-stop com.hobot.sample.app");

        startService(new Intent(this, MdvrService.class));
        Notify.get().registerListener(this);

        for (int i = 0; i < Config.MAX_CAM; i++) {
            mGlVideoViews[i] = findViewById(mGlVideoViewIds[i]);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.hardware.usb.action.USB_STATE");
        //registerReceiver(recevier, intentFilter);
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
        //unregisterReceiver(recevier);
        Notify.get().unregisterListener(this);
        stopService(new Intent(this, MdvrService.class));
        FlyLog.d("MdvrActiviy exit!");
    }

    @Override
    public void notify(byte[] data, int size) {

    }

    @Override
    public void handle(int type, byte[] data, int size, byte[] params) {
        if (type == NotifyType.NOTI_CAMOUT_YUV) {
            int channel = ByteUtil.bytes2Short(params, 0, true);
            int width = ByteUtil.bytes2Short(params, 2, true);
            int height = ByteUtil.bytes2Short(params, 4, true);
//            long ptsUsec = ByteUtil.bytes2Long(params, 6, true);
            mGlVideoViews[channel].upFrame(data, size, width, height);
        }
    }

}