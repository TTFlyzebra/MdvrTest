package com.flyzebra.mdvr;

import static com.flyzebra.mdvr.Config.MAX_CAM;
import static com.flyzebra.mdvr.Config.RTMP_URL;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.flyzebra.mdvr.audio.AudioService;
import com.flyzebra.mdvr.camera.CameraService;
import com.flyzebra.mdvr.net.RtmpService;
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

    private final CameraService cameraService = new CameraService(this);
    private final AudioService audioService = new AudioService(this);
    private final RtmpService[] rtmpServices = new RtmpService[MAX_CAM];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        for (int i = 0; i < MAX_CAM; i++) {
            mGlVideoViews[i] = findViewById(mGlVideoViewIds[i]);
        }
        Notify.get().registerListener(this);

        for (int i = 0; i < MAX_CAM; i++) {
            rtmpServices[i] = new RtmpService(i);
            rtmpServices[i].start(RTMP_URL+"/camera"+i);
        }

        cameraService.onCreate(Config.CAM_WIDTH, Config.CAM_HEIGHT);
        audioService.onCreate(Config.MIC_SAMPLE, Config.MIC_CHANNEL, Config.MIC_FORMAT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Notify.get().unregisterListener(this);

        for (int i = 0; i < MAX_CAM; i++) {
            rtmpServices[i].stop();
        }

        audioService.onDistory();
        cameraService.onDerstory();
        FlyLog.d("MdvrTest exit!");
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
            mGlVideoViews[channel].pushNv21data(data, size, width, height);
        }
    }
}