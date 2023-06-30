package com.flyzebra.mdvr;

import static com.flyzebra.mdvr.Config.MAX_CAM;
import static com.flyzebra.mdvr.Config.RTMP_URL;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.flyzebra.core.notify.INotify;
import com.flyzebra.mdvr.audio.AacService;
import com.flyzebra.mdvr.audio.PcmService;
import com.flyzebra.mdvr.camera.AvcService;
import com.flyzebra.mdvr.camera.YuvService;
import com.flyzebra.mdvr.rtmp.PusherService;
import com.flyzebra.utils.FlyLog;

public class MdvrService extends Service implements INotify {
    private final YuvService yuvServices = new YuvService(this);
    private final AvcService[] avcServices = new AvcService[MAX_CAM];
    private final PcmService pcmServices = new PcmService(this);
    private final AacService[] aacServices = new AacService[MAX_CAM];
    private final PusherService[] rtmpPushers = new PusherService[MAX_CAM];

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        FlyLog.d("MdvrService start!");
        yuvServices.onCreate(Config.CAM_WIDTH, Config.CAM_HEIGHT);
        pcmServices.onCreate(Config.MIC_SAMPLE, Config.MIC_CHANNEL, Config.MIC_FORMAT);

        for (int i = 0; i < MAX_CAM; i++) {
            avcServices[i] = new AvcService(i);
            aacServices[i] = new AacService(i);
        }

        for (int i = 0; i < MAX_CAM; i++) {
            rtmpPushers[i] = new PusherService(i);
            rtmpPushers[i].onCreate(RTMP_URL + "/camera" + i);
        }

        for (int i = 0; i < MAX_CAM; i++) {
            avcServices[i].onCreate();
        }

        for (int i = 0; i < MAX_CAM; i++) {
            aacServices[i].onCreate();
        }
    }

    @Override
    public void onDestroy() {
        FlyLog.d("MdvrService will exit!");
        yuvServices.onDerstory();
        pcmServices.onDistory();

        for (int i = 0; i < MAX_CAM; i++) {
            avcServices[i].onDistory();
        }

        for (int i = 0; i < MAX_CAM; i++) {
            aacServices[i].onDistory();
        }
        for (int i = 0; i < MAX_CAM; i++) {
            rtmpPushers[i].onDestory();
        }
        FlyLog.d("MdvrService exit!");
    }

    @Override
    public void notify(byte[] data, int size) {

    }

    @Override
    public void handle(int type, byte[] data, int size, byte[] params) {

    }
}
