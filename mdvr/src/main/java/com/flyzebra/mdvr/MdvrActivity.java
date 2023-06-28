package com.flyzebra.mdvr;

import static com.flyzebra.mdvr.Config.MAX_CAM;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.flyzebra.mdvr.opengl.GlVideoView;
import com.flyzebra.notify.INotify;
import com.flyzebra.notify.Notify;
import com.flyzebra.notify.NotifyType;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

public class MdvrActivity extends AppCompatActivity implements INotify {

    static {
        System.loadLibrary("mmqcar_qcar_jni");
    }

    private final GlVideoView[] mGlVideoViews = new GlVideoView[MAX_CAM];
    private final int[] mGlVideoViewIds = new int[]{R.id.sv01, R.id.sv02, R.id.sv03, R.id.sv04};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FlyLog.d("MdvrActiviy start!");

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        startService(new Intent(this, MdvrService.class));
        Notify.get().registerListener(this);

        for (int i = 0; i < MAX_CAM; i++) {
            mGlVideoViews[i] = findViewById(mGlVideoViewIds[i]);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
            mGlVideoViews[channel].pushNv12data(data, size, width, height);
        }
    }
}