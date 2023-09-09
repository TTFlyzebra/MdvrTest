package com.flyzebra.mdvr;

import com.arcsoft.visdrive.sdk.model.adas.ArcADASCalibInfo;
import com.arcsoft.visdrive.sdk.model.adas.ArcADASCalibResult;
import com.quectel.qcarapi.stream.QCarCamera;

import java.util.Hashtable;

public class Global {
    public static final Hashtable<Integer, byte[]> videoHeadMap = new Hashtable<>();
    public static final Hashtable<Integer, byte[]> audioHeadMap = new Hashtable<>();
    public static final Hashtable<Integer, QCarCamera> qCarCameras = new Hashtable<>();

    public static ArcADASCalibInfo calibInfo = new ArcADASCalibInfo();
    public static ArcADASCalibResult calibResult = new ArcADASCalibResult();
}
