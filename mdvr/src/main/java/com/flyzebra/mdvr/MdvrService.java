package com.flyzebra.mdvr;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.flyzebra.core.Fzebra;
import com.flyzebra.core.notify.INotify;
import com.flyzebra.mdvr.camera.CameraService;
import com.flyzebra.mdvr.rtmp.RtmpService;
import com.flyzebra.mdvr.sound.SoundService;
import com.flyzebra.utils.FlyLog;

public class MdvrService extends Service implements INotify {
    private final RtmpService rtmpService = new RtmpService(this);
    private final CameraService cameraService = new CameraService(this);
    private final SoundService soundService = new SoundService(this);

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
        rtmpService.onCreate();
        cameraService.onCreate();
        soundService.onCreate();
    }

    @Override
    public void onDestroy() {
        Fzebra.get().release();
        Fzebra.get().disableRtspServer();
        rtmpService.onDestory();
        cameraService.onDerstory();
        soundService.onDistory();
        FlyLog.d("MdvrService exit!");
    }

    @Override
    public void notify(byte[] data, int size) {

    }

    @Override
    public void handle(int type, byte[] data, int size, byte[] params) {

    }
}
