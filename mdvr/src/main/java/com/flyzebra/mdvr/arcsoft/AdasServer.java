package com.flyzebra.mdvr.arcsoft;

import android.content.Context;
import android.os.SystemClock;

import com.flyzebra.arcsoft.ArcSoftActive;
import com.flyzebra.arcsoft.ArcSoftAdas;
import com.flyzebra.mdvr.Config;
import com.flyzebra.mdvr.Global;
import com.flyzebra.utils.FlyLog;
import com.quectel.qcarapi.stream.QCarCamera;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdasServer {
    private int mChannel = 0;
    private Context mContext;
    private AdasThread adasThread = null;
    private ArcSoftAdas arcSoftAdas = null;
    private AtomicBoolean is_stop = new AtomicBoolean(true);

    public AdasServer(Context context) {
        mContext = context;
    }

    public void start() {
        if (!ArcSoftActive.get().isAdasActive()) {
            FlyLog.e("ADAS don't active！");
            return;
        }
        is_stop.set(false);
        adasThread = new AdasThread(mChannel);
        adasThread.start();
    }

    public void stop() {
        try {
            is_stop.set(true);
            adasThread.join();
            adasThread = null;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                qCarCamera = Global.qCarCameras.get(1);
            }

            int width = 1280;
            int height = 720;
            final int size = width * height * 3 / 2;
            ByteBuffer buffer = ByteBuffer.wrap(new byte[size]);

            qCarCamera.setSubStreamSize(channel, width, height);
            qCarCamera.startSubStream(channel);
            long frame_time = 1000L / Config.ADAS_FRAME_RATE;
            long last_time = SystemClock.uptimeMillis() - frame_time;

            if (arcSoftAdas == null) {
                arcSoftAdas = new ArcSoftAdas(mContext);
            }
            if (!arcSoftAdas.initAdas()) {
                FlyLog.i("AdasServer isn't active!");
                return;
            }

            arcSoftAdas.initAdasParam();

            while (!is_stop.get()) {
                QCarCamera.FrameInfo info = qCarCamera.getSubFrameInfo(channel, buffer);
                if (info != null) {
                    //FlyLog.e("sub camera=%d ptsSec=%d,ptsUsec=%d,frameID=%d", channel, info.ptsSec, info.ptsUsec, info.frameID);
                    arcSoftAdas.detectNV12(buffer, width * height * 3 / 2, width, height);
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

            if (arcSoftAdas != null) {
                arcSoftAdas.unInitAdas();
                arcSoftAdas = null;
            }
        }
    }
}
