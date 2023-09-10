package com.flyzebra.mdvr.arcsoft;

import android.content.Context;
import android.os.SystemClock;

import com.arcsoft.visdrive.sdk.model.dms.ArcDMSDetectResult;
import com.flyzebra.arcsoft.ArcSoftActive;
import com.flyzebra.arcsoft.ArcSoftDms;
import com.flyzebra.mdvr.Config;
import com.flyzebra.mdvr.Global;
import com.flyzebra.utils.FlyLog;
import com.quectel.qcarapi.stream.QCarCamera;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class DmsService {
    private int mChannel = 0;
    private Context mContext;
    private DmsThread dmsThread = null;
    private ArcSoftDms arcSoftDms = null;
    private AtomicBoolean is_stop = new AtomicBoolean(true);

    private DmsService() {
    }

    private static class DmsServiceHolder {
        public static final DmsService sInstance = new DmsService();
    }

    public static DmsService get() {
        return DmsService.DmsServiceHolder.sInstance;
    }

    public void init(Context context) {
        mContext = context;
    }

    public void start() {
        FlyLog.d("DmsService start!");
        is_stop.set(false);
        dmsThread = new DmsThread(mChannel);
        dmsThread.start();
    }

    public void stop() {
        is_stop.set(true);
        if (dmsThread != null) {
            try {
                dmsThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            dmsThread = null;
        }
        FlyLog.d("DmsService stop!");
    }

    private class DmsThread extends Thread implements Runnable {
        private final int channel;
        private QCarCamera qCarCamera;

        public DmsThread(int number) {
            this.channel = number;
            setName("dms-" + number);
        }

        @Override
        public void run() {
            while (!is_stop.get() && qCarCamera == null) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                qCarCamera = Global.qCarCameras.get(1);
            }

            while (!is_stop.get()) {
                if (ArcSoftActive.get().isDmsActive()) {
                    break;
                } else {
                    FlyLog.w("DMS don't active！");
                    ArcSoftActive.get().active(mContext.getApplicationContext(), Config.appId, Config.appSecret);
                    try {
                        for (int i = 0; i < 15; i++) {
                            if (is_stop.get()) return;
                            Thread.sleep(200);
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            //加载报警声音
            AlertMusicPlayer.get().loadDmsMusic(mContext);

            int width = 1280;
            int height = 720;
            final int size = width * height * 3 / 2;
            ByteBuffer buffer = ByteBuffer.allocateDirect(size);

            qCarCamera.setSubStreamSize(channel, width, height);
            qCarCamera.startSubStream(channel);

            long frame_time = 1000L / Config.DMS_FRAME_RATE;
            long last_time = SystemClock.uptimeMillis() - frame_time;

            if (arcSoftDms == null) {
                arcSoftDms = new ArcSoftDms(mContext);
            }
            if (!arcSoftDms.initDms()) {
                FlyLog.i("AdasService isn't active!");
                return;
            }

            arcSoftDms.initDmsParam();

            while (!is_stop.get()) {
                QCarCamera.FrameInfo info = qCarCamera.getSubFrameInfo(channel, buffer);
                if (info != null) {
                    ArcDMSDetectResult result = arcSoftDms.detectNV12(buffer, width * height * 3 / 2, width, height);
                    if (result != null) AlertMusicPlayer.get().playDms(result.alarmMask);
                } else {
                    FlyLog.e("Camera getVideoFrameInfo return null!");
                }
                //control fps
                long current_time = SystemClock.uptimeMillis();
                long sleep_time = frame_time - (current_time - last_time);
                if (sleep_time > 0 && sleep_time < frame_time) {
                    try {
                        Thread.sleep(sleep_time);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                last_time = SystemClock.uptimeMillis();
            }

            qCarCamera.stopSubStream(mChannel);

            if (arcSoftDms != null) {
                arcSoftDms.unInitAdas();
                arcSoftDms = null;
            }
        }
    }
}
