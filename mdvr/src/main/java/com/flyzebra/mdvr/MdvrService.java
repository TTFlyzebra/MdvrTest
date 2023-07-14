package com.flyzebra.mdvr;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.flyzebra.core.Fzebra;
import com.flyzebra.core.notify.INotify;
import com.flyzebra.mdvr.camera.CameraService_1080;
import com.flyzebra.mdvr.mic.MicService;
import com.flyzebra.mdvr.rtmp.RtmpService;
import com.flyzebra.utils.FlyLog;

public class MdvrService extends Service implements INotify {
    private final RtmpService rtmpService = new RtmpService(this);
    private final CameraService_1080 cameraService = new CameraService_1080(this);
    //private final CameraService_720 cameraService = new CameraService_720(this);
    private final MicService micService = new MicService(this);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        FlyLog.d("MdvrService start!");
        Fzebra.get().init();
        Fzebra.get().enableRtspServer();
        //start object
        //rtmpService.onCreate();
        cameraService.onCreate();
        micService.onCreate();
    }

    @Override
    public void onDestroy() {
        //rtmpService.onDestory();
        cameraService.onDerstory();
        micService.onDistory();
        Fzebra.get().disableRtspServer();
        Fzebra.get().release();
        FlyLog.d("MdvrService exit!");
    }

    @Override
    public void notify(byte[] data, int size) {

    }

    @Override
    public void handle(int type, byte[] data, int size, byte[] params) {

    }
}
