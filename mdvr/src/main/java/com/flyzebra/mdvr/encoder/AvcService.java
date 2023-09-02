package com.flyzebra.mdvr.encoder;

import android.content.Context;

import com.flyzebra.mdvr.Config;
import com.flyzebra.utils.FlyLog;

public class AvcService {

    private final AvcTasker[] avcTaskers = new AvcTasker[Config.MAX_CAM];

    public AvcService(Context context) {

    }

    public void start() {
        FlyLog.d("AvcService start!");

        for (int i = 0; i < Config.MAX_CAM; i++) {
            avcTaskers[i] = new AvcTasker(i,
                    Config.CAM_WIDTH,
                    Config.CAM_HEIGHT,
                    Config.FRAME_RATE,
                    Config.I_FRAME_INTERVAL,
                    Config.BIT_RATE,
                    Config.BITRATE_MODE);
            avcTaskers[i].start();
        }
    }

    public void stop() {
        for (int i = 0; i < Config.MAX_CAM; i++) {
            avcTaskers[i].stop();
        }
        FlyLog.d("AvcService stop!");
    }

}
