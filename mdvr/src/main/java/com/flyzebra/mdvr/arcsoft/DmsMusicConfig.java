package com.flyzebra.mdvr.arcsoft;

import android.content.Context;
import android.media.SoundPool;
import android.text.TextUtils;

import java.io.File;

public enum DmsMusicConfig {
    MOD_DMS_CALL("", "music/dca.mp3"),
    MOD_DMS_SMOKE("", "music/dsa.mp3"),
    MOD_DMS_CLOSE_EYE("", "music/dfw.mp3"),
    MOD_DMS_YAWN("", "music/dya.mp3"),
    MOD_DMS_DISTRACT("", "music/ddw.mp3"),
    MOD_DMS_DRIVER_ABNORMAL("", "music/daa.mp3"),
    MOD_DMS_IR_BLOCKING("", "music/dmscover.mp3"),
    MOD_DMS_LENS_COVERED("", "music/irblk.mp3"),
    MOD_DMS_SEATBELT_UNFASTENED("", "music/dsba.mp3");

    private final String path1; //sdcard
    private final String path2; //asset

    DmsMusicConfig(String path1, String path2) {
        this.path1 = path1;
        this.path2 = path2;
    }

    public int getId(Context context, SoundPool sp) {
        int id = -1;
        if (!TextUtils.isEmpty(path1) && new File(path1).exists()) {
            id = sp.load(path1, 1);
        } else {
            try {
                id = sp.load(context.getAssets().openFd(path2), 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return id;
    }
}
