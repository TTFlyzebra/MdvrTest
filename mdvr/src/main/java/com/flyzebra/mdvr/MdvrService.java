package com.flyzebra.mdvr;

import static com.flyzebra.mdvr.Config.MAX_CAM;
import static com.flyzebra.mdvr.Config.RTMP_URL;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.flyzebra.mdvr.audio.AacService;
import com.flyzebra.mdvr.audio.PcmService;
import com.flyzebra.mdvr.camera.AvcService;
import com.flyzebra.mdvr.camera.YuvService;
import com.flyzebra.mdvr.rtmp.PusherService;
import com.flyzebra.notify.INotify;
import com.flyzebra.utils.FlyLog;

public class MdvrService extends Service implements INotify {
    private final YuvService cameraRecorderService = new YuvService(this);
    private final AvcService[] cameraEncoderServices = new AvcService[MAX_CAM];
    private final PcmService audioRecorderService = new PcmService(this);
    private final AacService[] audioEncoderServices = new AacService[MAX_CAM];
    private final PusherService[] rtmpPusherServices = new PusherService[MAX_CAM];

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        FlyLog.d("MdvrService start!");

        for (int i = 0; i < MAX_CAM; i++) {
            cameraEncoderServices[i] = new AvcService(i);
            audioEncoderServices[i] = new AacService(i);
            rtmpPusherServices[i] = new PusherService(i);
        }

        for (int i = 0; i < MAX_CAM; i++) {
            cameraEncoderServices[i].onCreate();
        }

        for (int i = 0; i < MAX_CAM; i++) {
            audioEncoderServices[i].onCreate();
        }

        for (int i = 0; i < MAX_CAM; i++) {
            rtmpPusherServices[i].onCreate(RTMP_URL + "/camera" + i);
        }

        cameraRecorderService.onCreate(Config.CAM_WIDTH, Config.CAM_HEIGHT);
        audioRecorderService.onCreate(Config.MIC_SAMPLE, Config.MIC_CHANNEL, Config.MIC_FORMAT);
    }

    @Override
    public void onDestroy() {
        for (int i = 0; i < MAX_CAM; i++) {
            cameraEncoderServices[i].onDistory();
        }
        for (int i = 0; i < MAX_CAM; i++) {
            audioEncoderServices[i].onDistory();
        }
        for (int i = 0; i < MAX_CAM; i++) {
            rtmpPusherServices[i].onDestory();
        }
        cameraRecorderService.onDerstory();
        audioRecorderService.onDistory();
        FlyLog.d("MdvrService exit!");
    }

    @Override
    public void notify(byte[] data, int size) {

    }

    @Override
    public void handle(int type, byte[] data, int size, byte[] params) {

    }
}
