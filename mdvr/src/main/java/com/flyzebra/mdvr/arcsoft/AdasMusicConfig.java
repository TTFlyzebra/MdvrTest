package com.flyzebra.mdvr.arcsoft;

import android.content.Context;
import android.media.SoundPool;
import android.text.TextUtils;

import java.io.File;

public enum AdasMusicConfig {
    MOD_ADAS_LDW("", "music/ldw.mp3"),
    MOD_ADAS_FCW("", "music/fcw.mp3"),
    MOD_ADAS_HMW("", "music/hmw.mp3"),
    MOD_ADAS_PCW("", "music/pcw.mp3");

    private final String path1; //sdcard
    private final String path2; //asset

    AdasMusicConfig(String path1, String path2) {
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
