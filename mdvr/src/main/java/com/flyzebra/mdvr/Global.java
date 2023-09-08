package com.flyzebra.mdvr;

import com.arcsoft.visdrive.sdk.model.adas.ArcADASCalibInfo;
import com.quectel.qcarapi.stream.QCarCamera;

import java.util.Hashtable;

public class Global {
    public static final Hashtable<Integer, byte[]> videoHeadMap = new Hashtable<>();
    public static final Hashtable<Integer, byte[]> audioHeadMap = new Hashtable<>();
    public static final Hashtable<Integer, QCarCamera> qCarCameras = new Hashtable<>();

    public static ArcADASCalibInfo calibInfo = new ArcADASCalibInfo();
    static {
        calibInfo.horizon = 380;
        calibInfo.carMiddle = 35;
        calibInfo.cameraToAxle = 73;
        calibInfo.cameraToBumper = 120;
        calibInfo.cameraHeight = 135;
        calibInfo.carWidth = 192;
    }
}
