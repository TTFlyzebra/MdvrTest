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
import com.flyzebra.mdvr.arcsoft.AdasServer;
import com.flyzebra.mdvr.arcsoft.DmsServer;
import com.flyzebra.mdvr.camera.CamServer;
import com.flyzebra.mdvr.encoder.AacServer;
import com.flyzebra.mdvr.encoder.AvcServer;
import com.flyzebra.mdvr.mic.MicServer;
import com.flyzebra.mdvr.store.StorageServer;
import com.flyzebra.mdvr.wifi.WifiServer;
import com.flyzebra.mdvr.wifip2p.WifiP2PServer;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

public class MdvrService extends Service implements INotify {
    static {
        System.loadLibrary("mmqcar_qcar_jni");
    }
    private final StorageServer storeServer = new StorageServer(this);
    private final WifiServer wifiServer = new WifiServer(this);
    private final WifiP2PServer wifiP2PServer = new WifiP2PServer(this);
    //private final RtmpServer rtmpServer = new RtmpServer(this);
    private final CamServer camServer = new CamServer(this);
    private final MicServer micServer = new MicServer(this);

    private final AvcServer avcServer = new AvcServer(this);
    private final AacServer aacServer = new AacServer(this);

    private final AdasServer adasServer = new AdasServer(this);
    private final DmsServer dmsServer = new DmsServer(this);

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

        storeServer.start();
        wifiServer.start();
        wifiP2PServer.start();

        //rtmpServer.start();

        camServer.start();
        micServer.start();

        aacServer.start();
        avcServer.start();

        adasServer.start();
        dmsServer.start();
    }

    @Override
    public void onDestroy() {
        Notify.get().unregisterListener(this);
        Fzebra.get().stopRtspServer();
        Fzebra.get().release();

        storeServer.stop();
        wifiServer.stop();
        wifiP2PServer.stop();

        //rtmpServer.stop();

        adasServer.stop();//必须在camServer前面停止
        dmsServer.stop();//必须在camServer前面停止

        aacServer.stop();
        avcServer.stop();

        camServer.stop();
        micServer.stop();

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
