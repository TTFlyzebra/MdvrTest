package com.flyzebra.mdvr;

import android.app.Application;

import com.flyzebra.arcsoft.ArcSoftActive;
import com.flyzebra.mdvr.arcsoft.AdasService;
import com.flyzebra.mdvr.arcsoft.DmsService;
import com.flyzebra.utils.FlyLog;
import com.flyzebra.utils.SPUtil;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FlyLog.d("##############MDVR Version 1.0.0##############");

        ArcSoftActive.get().init(getApplicationContext());
        ArcSoftActive.get().active(getApplicationContext(), Config.appId, Config.appSecret);

        AdasService.get().init(getApplicationContext());
        DmsService.get().init(getApplicationContext());

        try {
            Global.calibInfo.horizon = (int) SPUtil.get(getApplicationContext(), "calibInfo_horizon", 380);
            Global.calibInfo.carMiddle = (int) SPUtil.get(getApplicationContext(), "calibInfo_carMiddle", 35);
            Global.calibInfo.cameraToAxle = (int) SPUtil.get(getApplicationContext(), "calibInfo_cameraToAxle", 73);
            Global.calibInfo.cameraToBumper = (int) SPUtil.get(getApplicationContext(), "calibInfo_cameraToBumper", 120);
            Global.calibInfo.cameraHeight = (int) SPUtil.get(getApplicationContext(), "calibInfo_cameraHeight", 135);
            Global.calibInfo.carWidth = (int) SPUtil.get(getApplicationContext(), "calibInfo_carWidth", 192);
            Global.calibInfo.cameraToLeftWheel = (int) SPUtil.get(getApplicationContext(), "calibInfo_cameraToLeftWheel", 91);
            String r1 = (String) SPUtil.get(getApplicationContext(), "calibResult_r1", "-1.179168");
            String r2 = (String) SPUtil.get(getApplicationContext(), "calibResult_r2", "1.1453419");
            String r3 = (String) SPUtil.get(getApplicationContext(), "calibResult_r3", "-1.214212");
            String t1 = (String) SPUtil.get(getApplicationContext(), "calibResult_t1", "0.0");
            String t2 = (String) SPUtil.get(getApplicationContext(), "calibResult_t2", "0.0");
            String t3 = (String) SPUtil.get(getApplicationContext(), "calibResult_t3", "1350.0");
            String pitch = (String) SPUtil.get(getApplicationContext(), "calibResult_pitch", "3.3437219");
            String yaw = (String) SPUtil.get(getApplicationContext(), "calibResult_yaw", "-1.6674138");
            String roll = (String) SPUtil.get(getApplicationContext(), "calibResult_roll", "0.0");
            Global.calibResult.r1 = Double.parseDouble(r1);
            Global.calibResult.r2 = Double.parseDouble(r2);
            Global.calibResult.r3 = Double.parseDouble(r3);
            Global.calibResult.t1 = Double.parseDouble(t1);
            Global.calibResult.t2 = Double.parseDouble(t2);
            Global.calibResult.t3 = Double.parseDouble(t3);
            Global.calibResult.pitch = Double.parseDouble(pitch);
            Global.calibResult.yaw = Double.parseDouble(yaw);
            Global.calibResult.roll = Double.parseDouble(roll);
        } catch (Exception e) {
            FlyLog.e(e.toString());
        }
    }
}
