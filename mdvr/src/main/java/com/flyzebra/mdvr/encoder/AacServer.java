package com.flyzebra.mdvr.encoder;


import android.content.Context;

import com.flyzebra.mdvr.Config;
import com.flyzebra.utils.FlyLog;

public class AacServer {

    private final AacTasker[] aacTaskers = new AacTasker[Config.MAX_CAM];

    public AacServer(Context context){

    }

    public void start() {
        FlyLog.d("AacServer start!");
        for (int i = 0; i < Config.MAX_CAM; i++) {
            aacTaskers[i] = new AacTasker(i);
            aacTaskers[i].start();
        }
    }

    public void stop() {
        for (int i = 0; i < Config.MAX_CAM; i++) {
            aacTaskers[i].stop();
        }
        FlyLog.d("AacServer stop!");
    }
}
