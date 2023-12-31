package com.flyzebra.mdvr.arcsoft;

import android.content.Context;
import android.os.SystemClock;

import com.arcsoft.visdrive.sdk.model.adas.ArcADASCalibInfo;
import com.arcsoft.visdrive.sdk.model.adas.ArcADASCalibResult;
import com.arcsoft.visdrive.sdk.model.adas.ArcADASDetectResult;
import com.flyzebra.arcsoft.ArcSoftActive;
import com.flyzebra.arcsoft.ArcSoftAdas;
import com.flyzebra.mdvr.Config;
import com.flyzebra.mdvr.Global;
import com.flyzebra.utils.FlyLog;
import com.quectel.qcarapi.stream.QCarCamera;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdasService {
    private int mChannel = 1;
    private Context mContext;
    private AdasThread adasThread = null;
    private ArcSoftAdas arcSoftAdas = null;
    private AtomicBoolean is_stop = new AtomicBoolean(true);

    private AdasService() {
    }

    private static class AdasServiceHolder {
        public static final AdasService sInstance = new AdasService();
    }

    public static AdasService get() {
        return AdasService.AdasServiceHolder.sInstance;
    }

    public void init(Context context) {
        mContext = context;
    }

    public void start() {
        FlyLog.d("AdasService start!");
        is_stop.set(false);
        adasThread = new AdasThread(mChannel);
        adasThread.start();
    }

    public void stop() {
        is_stop.set(true);
        if (adasThread != null) {
            try {
                adasThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            adasThread = null;
        }
        FlyLog.d("AdasService stop!");
    }

    public ArcADASCalibResult setCalibInfo(ArcADASCalibInfo calibInfo) {
        if (arcSoftAdas != null) {
            return arcSoftAdas.setAdasCalibInfo(calibInfo);
        } else {
            return null;
        }
    }

    private class AdasThread extends Thread implements Runnable {
        private final int channel;
        private QCarCamera qCarCamera;

        public AdasThread(int number) {
            this.channel = number;
            setName("adas-" + number);
        }

        @Override
        public void run() {
            while (!is_stop.get() && qCarCamera == null) {
                try {
                    for (int i = 0; i < 15; i++) {
                        if (is_stop.get()) return;
                        Thread.sleep(200);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                qCarCamera = Global.qCarCameras.get(1);
            }

            while (!is_stop.get()) {
                if (ArcSoftActive.get().isAdasActive()) {
                    break;
                } else {
                    FlyLog.e("ADAS don't active！");
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
            AlertMusicPlayer.get().loadAdasMusic(mContext);

            int width = 1280;
            int height = 720;
            final int size = width * height * 3 / 2;

            ByteBuffer buffer = ByteBuffer.allocateDirect(size);

            qCarCamera.setSubStreamSize(channel, width, height);
            qCarCamera.startSubStream(channel);
            long frame_time = 1000L / Config.ADAS_FRAME_RATE;
            long last_time = SystemClock.uptimeMillis() - frame_time;

            if (arcSoftAdas == null) {
                arcSoftAdas = new ArcSoftAdas(mContext);
            }
            if (!arcSoftAdas.initAdas(Global.calibInfo, Global.calibResult)) {
                FlyLog.i("AdasService isn't active!");
                return;
            }

            arcSoftAdas.initAdasParam();

            while (!is_stop.get()) {
                QCarCamera.FrameInfo info = qCarCamera.getSubFrameInfo(channel, buffer);
                if (info != null) {
                    ArcADASDetectResult result = arcSoftAdas.detectNV12(buffer, width * height * 3 / 2, width, height);
                    if (result != null) AlertMusicPlayer.get().playAdas(result.alarmMask);
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

            if (arcSoftAdas != null) {
                arcSoftAdas.unInitAdas();
                arcSoftAdas = null;
            }
        }
    }
}
