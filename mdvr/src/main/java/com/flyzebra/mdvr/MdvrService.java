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
import com.flyzebra.mdvr.arcsoft.AdasService;
import com.flyzebra.mdvr.arcsoft.DmsService;
import com.flyzebra.mdvr.camera.CamService;
import com.flyzebra.mdvr.encoder.AacService;
import com.flyzebra.mdvr.encoder.AvcService;
import com.flyzebra.mdvr.mic.MicService;
import com.flyzebra.mdvr.store.StorageService;
import com.flyzebra.mdvr.wifi.WifiService;
import com.flyzebra.mdvr.wifip2p.WifiP2PService;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

public class MdvrService extends Service implements INotify {
    static {
        System.loadLibrary("mmqcar_qcar_jni");
    }
    private final StorageService storeService = new StorageService(this);
    private final WifiService wifiService = new WifiService(this);
    private final WifiP2PService wifiP2PService = new WifiP2PService(this);
    //private final RtmpService rtmpServer = new RtmpService(this);
    private final CamService camService = new CamService(this);
    private final MicService micService = new MicService(this);

    private final AvcService avcService = new AvcService(this);
    private final AacService aacService = new AacService(this);

    private final AdasService adasService = new AdasService(this);
    private final DmsService dmsService = new DmsService(this);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        FlyLog.i("#####MdvrService start!#####");

        AlarmManager mAlarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        mAlarmManager.setTimeZone("Asia/Shanghai");

        Notify.get().registerListener(this);

        Global.qCarCameras.clear();
        Global.audioHeadMap.clear();
        Global.videoHeadMap.clear();

        Fzebra.get().init();
        Fzebra.get().startRtspServer();

        storeService.start();
        wifiService.start();
        wifiP2PService.start();

        //rtmpServer.start();

        camService.start();
        micService.start();

        aacService.start();
        avcService.start();

        adasService.start();
        dmsService.start();
    }

    @Override
    public void onDestroy() {
        Notify.get().unregisterListener(this);
        Fzebra.get().stopRtspServer();
        Fzebra.get().release();

        storeService.stop();
        wifiService.stop();
        wifiP2PService.stop();

        //rtmpServer.stop();

        adasService.stop();//必须在camServer前面停止
        dmsService.stop();//必须在camServer前面停止

        aacService.stop();
        avcService.stop();

        camService.stop();
        micService.stop();

        FlyLog.i("#####MdvrService exit!#####");
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
