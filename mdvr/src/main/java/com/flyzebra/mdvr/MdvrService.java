package com.flyzebra.mdvr;

import android.app.AlarmManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.flyzebra.core.Fzebra;
import com.flyzebra.core.notify.INotify;
import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.NotifyType;
import com.flyzebra.mdvr.camera.CamServer;
import com.flyzebra.mdvr.mic.MicServer;
import com.flyzebra.mdvr.rtmp.RtmpServer;
import com.flyzebra.mdvr.store.StorageServer;
import com.flyzebra.mdvr.wifi.WifiService;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

public class MdvrService extends Service implements INotify {
    static {
        System.loadLibrary("mmqcar_qcar_jni");
    }
    private final WifiService wifiServer = new WifiService(this);
    private final StorageServer storeServer = new StorageServer(this);
    private final RtmpServer rtmpServer = new RtmpServer(this);
    private final CamServer cameraServer = new CamServer(this);
    //private final CamServer1080P cameraServer = new CamServer1080P(this);
    private final MicServer micServer = new MicServer(this);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        FlyLog.d("MdvrService start!");

        AlarmManager mAlarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        mAlarmManager.setTimeZone("Asia/Shanghai");

        Notify.get().registerListener(this);
        Global.qCarCameras.clear();
        Global.audioHeadMap.clear();
        Global.videoHeadMap.clear();

        Fzebra.get().init();
        Fzebra.get().startRtspServer();
        wifiServer.start();
        storeServer.start();
        rtmpServer.start();
        cameraServer.start();
        micServer.start();
    }

    @Override
    public void onDestroy() {
        wifiServer.stop();
        storeServer.stop();
        rtmpServer.stop();
        cameraServer.stop();
        micServer.stop();
        Fzebra.get().stopRtspServer();
        Fzebra.get().release();
        Notify.get().unregisterListener(this);
        FlyLog.d("MdvrService exit!");
    }

    @Override
    public void notify(byte[] data, int size) {

    }

    @Override
    public void handle(int type, byte[] data, int size, byte[] params) {
        if (NotifyType.NOTI_MICOUT_SPS == type) {
            short channel = ByteUtil.bytes2Short(params, 0, true);
            byte[] audioHead = new byte[size];
            System.arraycopy(data, 0, audioHead, 0, size);
            Global.audioHeadMap.put((int) channel, audioHead);
        } else if (NotifyType.NOTI_CAMOUT_SPS == type) {
            short channel = ByteUtil.bytes2Short(params, 0, true);
            byte[] videoHead = new byte[size];
            System.arraycopy(data, 0, videoHead, 0, size);
            Global.videoHeadMap.put((int) channel, videoHead);
        }
    }
}
