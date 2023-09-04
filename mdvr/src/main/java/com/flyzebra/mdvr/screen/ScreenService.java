package com.flyzebra.mdvr.screen;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.Surface;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.flyzebra.core.notify.INotify;
import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.NotifyType;
import com.flyzebra.core.notify.Protocol;
import com.flyzebra.mdvr.R;
import com.flyzebra.mdvr.activiy.MdvrActivity;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;
import com.flyzebra.utils.IDUtil;

import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenService extends Service implements INotify {
    public static AtomicBoolean isApplyScreen = new AtomicBoolean(false);
    private final String NOTI_ID = "NOTIFICATION_ID_MCTL";
    private MediaProjectionManager mpManager = null;
    private int mResultCode;
    private Intent mResultData;
    private int width = 640;
    private int height = 1280;
    private int mBitRate = 4000000;
    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 25; // 30 fps
    private static final int IFRAME_INTERVAL = 5; // 2 seconds between I-frames
    private byte[] video_data = new byte[720 * 1440 * 3 / 2];
    private Thread workThread = null;
    private AtomicBoolean isStop = new AtomicBoolean(true);
    private Hashtable<Long, Long> mScreenUsers = new Hashtable<>();
    private long mTid = 0;

    private static final HandlerThread mCmdThread = new HandlerThread("screen_cmd");

    static {
        mCmdThread.start();
    }

    private static final Handler mCmdHandler = new Handler(mCmdThread.getLooper());

    public static void startService(AppCompatActivity activity) {
        if (!isApplyScreen.get()) {
            MediaProjectionManager mpManager = (MediaProjectionManager) activity.getSystemService(MEDIA_PROJECTION_SERVICE);
            activity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                int resultCode = result.getResultCode();
                Intent service = new Intent(activity, ScreenService.class);
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

    @Override
    public void onCreate() {
        mpManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mTid = Long.parseLong(IDUtil.getIMEI(this));
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext()); //获取一个Notification构造器
        Intent nfIntent = new Intent(this, MdvrActivity.class); //点击后跳转的界面，可以设置跳转数据
        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0)) // 设置PendingIntent
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher)) // 设置下拉列表中的图标(大图标)
                //.setContentTitle("SMI InstantView") // 设置下拉列表里的标题
                .setSmallIcon(R.mipmap.ic_launcher) // 设置状态栏内的小图标
                .setContentText("正在录屏......") // 设置上下文内容
                .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间

        /*以下是对Android 8.0的适配*/
        //普通notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(NOTI_ID);
        }
        //前台服务notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(NOTI_ID, "notification_name", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = builder.build(); // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音
        startForeground(1001, notification);
        Notify.get().registerListener(this);

        isApplyScreen.set(false);
    }

    @Override
    public void onDestroy() {
        mCmdHandler.removeCallbacksAndMessages(null);
        Notify.get().unregisterListener(this);
        isApplyScreen.set(false);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            try {
                boolean flag = intent.getBooleanExtra("screen", false);
                if (flag) {
                    mResultCode = intent.getIntExtra("code", -1);
                    mResultData = intent.getParcelableExtra("data");
                    isApplyScreen.set(true);
                }
            } catch (Exception e) {
                FlyLog.e(e.toString());
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void screenStart() {
        FlyLog.d("screenStart");
        isStop.set(false);
        workThread = new Thread(() -> {
            FlyLog.d("screen work thread start!");
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            MediaCodec codec = null;
            MediaProjection mMediaProjection = null;
            VirtualDisplay mVirtualDisplay = null;
            try {
                mMediaProjection = mpManager.getMediaProjection(mResultCode, mResultData);
                MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
                format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
                codec = MediaCodec.createEncoderByType(MIME_TYPE);
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                Surface surface = codec.createInputSurface();
                codec.start();
                mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                        "screen-display",
                        width,
                        height,
                        1,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                        surface,
                        null,
                        null);
                while (!isStop.get()) {
                    Iterator<Map.Entry<Long, Long>> it = mScreenUsers.entrySet().iterator();
                    long crt_time = SystemClock.uptimeMillis();
                    while (it.hasNext()) {
                        Map.Entry<Long, Long> entry = it.next();
                        if (crt_time - entry.getValue() > 15000) {
                            it.remove();
                            FlyLog.d("User timeout disconnect, %d", entry.getKey());
                        }
                    }
                    if (mScreenUsers.isEmpty()) {
                        isStop.set(true);
                        break;
                    }
                    int eobIndex = codec.dequeueOutputBuffer(bufferInfo, 1000);
                    if (isStop.get()) break;
                    if (eobIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat mediaFormat = codec.getOutputFormat();
                        ByteBuffer spsBuffer = mediaFormat.getByteBuffer("csd-0");
                        ByteBuffer ppsBuffer = mediaFormat.getByteBuffer("csd-1");
                        int spsLen = spsBuffer.remaining();
                        int ppsLen = ppsBuffer.remaining();
                        byte[] data = new byte[spsLen + ppsLen];
                        spsBuffer.get(data, 0, spsLen);
                        ppsBuffer.get(data, spsLen, ppsLen);
                        Notify.get().handledata(NotifyType.NOTI_SCREEN_SPS, data, spsLen, null);
                        break;
                    } else if (eobIndex >= 0) {
                        ByteBuffer data = codec.getOutputBuffer(eobIndex);
                        data.position(bufferInfo.offset);
                        data.limit(bufferInfo.offset + bufferInfo.size);
                        data.get(video_data, 0, bufferInfo.size);
                        Notify.get().handledata(NotifyType.NOTI_SCREEN_AVC, video_data, bufferInfo.size, null);
                        codec.releaseOutputBuffer(eobIndex, false);
                    }
                }
            } catch (Exception e) {
                FlyLog.e(e.toString());
            } finally {
                if (codec != null) {
                    codec.stop();
                    codec.release();
                }
                if (mVirtualDisplay != null) {
                    mVirtualDisplay.release();
                }
                if (mMediaProjection != null) {
                    mMediaProjection.stop();
                }
            }
            FlyLog.d("screen work thread exit!");
        }, "screen_encoder");
        workThread.start();
    }

    public void screenStop() {
        isStop.set(true);
        try {
            if (workThread != null) {
                workThread.join();
                workThread = null;
            }
        } catch (InterruptedException e) {
            FlyLog.e(e.toString());
        }
        FlyLog.d("screenStop");
    }

    @Override
    public void notify(byte[] data, int size) {
        mCmdHandler.post(() -> handleCmd(data, size));
    }

    @Override
    public void handle(int type, byte[] data, int size, byte[] params) {

    }

    private void handleCmd(byte[] data, int size) {
        short type = ByteUtil.bytes2Short(data, 2, true);
        switch (type) {
            case Protocol.TYPE_UT_HEARTBEAT: {
                long uid = ByteUtil.bytes2Long(data, 16, true);
                mScreenUsers.put(uid, SystemClock.uptimeMillis());
                break;
            }
            case Protocol.TYPE_U_DISCONNECTED: {
                long uid = ByteUtil.bytes2Long(data, 8, true);
                mScreenUsers.remove(uid);
                FlyLog.d("recv screen user is disconnect, client size[%d]", mScreenUsers.size());
                if (mScreenUsers.isEmpty()) screenStop();
                break;
            }
            case Protocol.TYPE_T_DISCONNECTED: {
                mScreenUsers.clear();
                screenStop();
                break;
            }
            case Protocol.TYPE_SCREEN_U_READY: {
                long uid = ByteUtil.bytes2Long(data, 16, true);
                mScreenUsers.put(uid, SystemClock.uptimeMillis());
                FlyLog.d("recv screen avc start, client size[%d]uid[%d]", mScreenUsers.size(), uid);
                Notify.get().miniNotify(Protocol.SCREEN_T_START, Protocol.SCREEN_T_START.length, mTid, uid, null);
                break;
            }
            case Protocol.TYPE_SCREEN_U_START: {
                screenStop();
                screenStart();
                break;
            }
            case Protocol.TYPE_SCREEN_U_STOP: {
                long uid = ByteUtil.bytes2Long(data, 16, true);
                mScreenUsers.remove(uid);
                FlyLog.d("recv screen avc stop, client size[%d]", mScreenUsers.size());
                if (mScreenUsers.isEmpty()) screenStop();
                break;
            }
        }
    }
}
