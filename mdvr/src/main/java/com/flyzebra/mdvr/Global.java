package com.flyzebra.mdvr;

import com.quectel.qcarapi.stream.QCarCamera;

import java.util.Hashtable;

public class Global {
    public static final Hashtable<Integer, byte[]> videoHeadMap = new Hashtable<>();
    public static final Hashtable<Integer, byte[]> audioHeadMap = new Hashtable<>();

    public static final Hashtable<Integer, QCarCamera> qCarCameras = new Hashtable<>();
}
