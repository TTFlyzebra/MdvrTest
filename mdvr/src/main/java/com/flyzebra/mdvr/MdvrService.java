package com.flyzebra.mdvr;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.flyzebra.core.Fzebra;
import com.flyzebra.core.notify.INotify;
import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.NotifyType;
import com.flyzebra.mdvr.activity.LauncherActivity;
import com.flyzebra.mdvr.arcsoft.AdasService;
import com.flyzebra.mdvr.arcsoft.DmsService;
import com.flyzebra.mdvr.camera.CamService;
import com.flyzebra.mdvr.encoder.AacService;
import com.flyzebra.mdvr.encoder.AvcService;
import com.flyzebra.mdvr.input.InputService;
import com.flyzebra.mdvr.mic.MicService;
import com.flyzebra.mdvr.record.RecordService;
import com.flyzebra.mdvr.screen.ScreenService;
import com.flyzebra.mdvr.wifi.WifiService;
import com.flyzebra.mdvr.wifip2p.WifiP2PService;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;
import com.flyzebra.utils.IDUtil;

import java.util.concurrent.atomic.AtomicBoolean;

public class MdvrService extends Service implements INotify {
    static {
        System.loadLibrary("mmqcar_qcar_jni");
    }

    private final String NOTI_ID = "NOTIFICATION_ID_MCTL";

    private final RecordService recordService = new RecordService(this);
    private final WifiService wifiService = new WifiService(this);
    private final WifiP2PService wifiP2PService = new WifiP2PService(this);
    //private final RtmpService rtmpServer = new RtmpService(this);
    private final CamService camService = new CamService(this);
    private final MicService micService = new MicService(this);

    private final AvcService avcService = new AvcService(this);
    private final AacService aacService = new AacService(this);

    private final AdasService adasService = AdasService.get();
    private final DmsService dmsService = DmsService.get();

    public static AtomicBoolean isApplyScreen = new AtomicBoolean(false);
    private final ScreenService screenService = new ScreenService(this);
    private final InputService inputService = new InputService(this);
    private static final Handler mHandler = new Handler(Looper.getMainLooper());

    private final Runnable checkImei = new Runnable() {
        @Override
        public void run() {
            if (!TextUtils.isEmpty(IDUtil.getIMEI(MdvrService.this))) {
                startServices();
            } else {
                mHandler.postDelayed(this, 1000);
            }
        }
    };

    public static void startSelfService(AppCompatActivity activity) {
        if (!isApplyScreen.get()) {
            MediaProjectionManager mpManager = (MediaProjectionManager) activity.getSystemService(MEDIA_PROJECTION_SERVICE);
            activity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                int resultCode = result.getResultCode();
                Intent service = new Intent(activity, MdvrService.class);
                service.putExtra("screen", true);
                service.putExtra("code", resultCode);
                service.putExtra("data", data);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    activity.startForegroundService(service);
                } else {
                    activity.startService(service);
                }
            }).launch(mpManager.createScreenCaptureIntent());
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        FlyLog.i("onCreate");

        Notify.get().registerListener(this);

        Fzebra.get().init(this.getApplicationContext());
        Fzebra.get().startUserServer();
        Fzebra.get().startRtspServer();

        Notification.Builder builder = new Notification.Builder(this.getApplicationContext());
        Intent nfIntent = new Intent(this, LauncherActivity.class);
        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0))
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
                .setContentTitle("Meitrack MDVR")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("MDVR后台运行中......")
                .setWhen(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(NOTI_ID);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(NOTI_ID, "notification_name", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
        Notification notification = builder.build();
        notification.defaults = Notification.DEFAULT_SOUND;
        startForeground(1001, notification);

        isApplyScreen.set(false);
        AlarmManager mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mAlarmManager.setTimeZone("Asia/Shanghai");

        Global.qCarCameras.clear();
        Global.audioHeadMap.clear();
        Global.videoHeadMap.clear();

        mHandler.post(checkImei);
    }

    public void startServices() {
        recordService.start();
        wifiService.start();
        wifiP2PService.start();

        //rtmpServer.start();

        camService.start();
        micService.start();

        aacService.start();
        avcService.start();

        adasService.start();
        dmsService.start();

        inputService.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (intent != null) {
                boolean flag = intent.getBooleanExtra("screen", false);
                if (flag) {
                    FlyLog.d("onStartCommand from Activity");
                    int mResultCode = intent.getIntExtra("code", -1);
                    Intent mResultData = intent.getParcelableExtra("data");
                    screenService.start(mResultCode, mResultData);
                    isApplyScreen.set(true);
                }
            }
        } catch (Exception e) {
            FlyLog.e(e.toString());
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Notify.get().unregisterListener(this);
        Fzebra.get().stopRtspServer();

        recordService.stop();
        wifiService.stop();
        wifiP2PService.stop();

        //rtmpServer.stop();

        adasService.stop();//必须在camServer前面停止
        dmsService.stop();//必须在camServer前面停止

        aacService.stop();
        avcService.stop();

        camService.stop();
        micService.stop();

        screenService.stop();
        inputService.stop();

        Fzebra.get().stopUserServer();
        Fzebra.get().release();
        FlyLog.i("onDestroy");
    }

    @Override
    public void notify(byte[] data, int size) {

    }

    @Override
    public void handle(int type, byte[] data, int dsize, byte[] params, int psize) {
        if (NotifyType.NOTI_MICOUT_SPS == type) {
            short channel = ByteUtil.bytes2Short(params, 0, true);
            byte[] audioHead = new byte[dsize];
            System.arraycopy(data, 0, audioHead, 0, dsize);
            Global.audioHeadMap.put((int) channel, audioHead);
        } else if (NotifyType.NOTI_CAMOUT_SPS == type) {
            short channel = ByteUtil.bytes2Short(params, 0, true);
            byte[] videoHead = new byte[dsize];
            System.arraycopy(data, 0, videoHead, 0, dsize);
            Global.videoHeadMap.put((int) channel, videoHead);
        }
    }
}
