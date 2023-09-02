package com.flyzebra.mdvr.encoder;


import android.content.Context;

import com.flyzebra.mdvr.Config;
import com.flyzebra.utils.FlyLog;

public class AacService {

    private final AacTasker[] aacTaskers = new AacTasker[Config.MAX_CAM];

    public AacService(Context context){

    }

    public void start() {
        FlyLog.d("AacService start!");
        for (int i = 0; i < Config.MAX_CAM; i++) {
            aacTaskers[i] = new AacTasker(i);
            aacTaskers[i].start();
        }
    }

    public void stop() {
        for (int i = 0; i < Config.MAX_CAM; i++) {
            aacTaskers[i].stop();
        }
        FlyLog.d("AacService stop!");
    }
}
