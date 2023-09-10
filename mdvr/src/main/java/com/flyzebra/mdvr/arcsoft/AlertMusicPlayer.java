/**
 * FileName: SoundPlayer
 * Author: FlyZebra
 * Email:flycnzebra@gmail.com
 * Date: 2023/9/10 9:41
 * Description:
 */
package com.flyzebra.mdvr.arcsoft;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;

import com.arcsoft.visdrive.sdk.constant.adas.ArcADASDetectMaskType;
import com.arcsoft.visdrive.sdk.constant.dms.ArcDMSDetectMaskType;

import java.util.Hashtable;

public class AlertMusicPlayer {
    private SoundPool splay = null;
    private final Hashtable<Integer, Integer> adasMap = new Hashtable<>();
    private final Hashtable<Integer, Integer> dmsMap = new Hashtable<>();

    private AlertMusicPlayer() {
    }

    private static class SoundPlayerHolder {
        public static final AlertMusicPlayer sInstance = new AlertMusicPlayer();
    }

    public static AlertMusicPlayer get() {
        return SoundPlayerHolder.sInstance;
    }

    private void init() {
        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setMaxStreams(5);
        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
        attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC);
        builder.setAudioAttributes(attrBuilder.build());
        splay = builder.build();
    }

    public void loadAdasMusic(Context context) {
        if (splay == null) init();
        adasMap.put(ArcADASDetectMaskType.MOD_ADAS_LDW, AdasMusicConfig.MOD_ADAS_LDW.getId(context, splay));
        adasMap.put(ArcADASDetectMaskType.MOD_ADAS_FCW, AdasMusicConfig.MOD_ADAS_FCW.getId(context, splay));
        adasMap.put(ArcADASDetectMaskType.MOD_ADAS_HMW, AdasMusicConfig.MOD_ADAS_HMW.getId(context, splay));
        adasMap.put(ArcADASDetectMaskType.MOD_ADAS_PCW, AdasMusicConfig.MOD_ADAS_PCW.getId(context, splay));
    }

    public void loadDmsMusic(Context context) {
        if (splay == null) init();
        dmsMap.put(ArcDMSDetectMaskType.MOD_DMS_CALL, DmsMusicConfig.MOD_DMS_CALL.getId(context, splay));
        dmsMap.put(ArcDMSDetectMaskType.MOD_DMS_SMOKE, DmsMusicConfig.MOD_DMS_SMOKE.getId(context, splay));
        dmsMap.put(ArcDMSDetectMaskType.MOD_DMS_CLOSE_EYE, DmsMusicConfig.MOD_DMS_CLOSE_EYE.getId(context, splay));
        dmsMap.put(ArcDMSDetectMaskType.MOD_DMS_YAWN, DmsMusicConfig.MOD_DMS_YAWN.getId(context, splay));
        dmsMap.put(ArcDMSDetectMaskType.MOD_DMS_DISTRACT, DmsMusicConfig.MOD_DMS_DISTRACT.getId(context, splay));
        dmsMap.put(ArcDMSDetectMaskType.MOD_DMS_DRIVER_ABNORMAL, DmsMusicConfig.MOD_DMS_DRIVER_ABNORMAL.getId(context, splay));
        dmsMap.put(ArcDMSDetectMaskType.MOD_DMS_IR_BLOCKING, DmsMusicConfig.MOD_DMS_IR_BLOCKING.getId(context, splay));
        dmsMap.put(ArcDMSDetectMaskType.MOD_DMS_LENS_COVERED, DmsMusicConfig.MOD_DMS_LENS_COVERED.getId(context, splay));
        dmsMap.put(ArcDMSDetectMaskType.MOD_DMS_SEATBELT_UNFASTENED, DmsMusicConfig.MOD_DMS_SEATBELT_UNFASTENED.getId(context, splay));
    }

    public void playAdas(int maskType) {
        if (splay == null) {
            return;
        }
        try {
            int id = adasMap.get(maskType);
            if (id >= 0) {
                splay.play(id, 1, 1, 1, 0, 1.0f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playDms(int maskType) {
        if (splay == null) {
            return;
        }
        try {
            int id = dmsMap.get(maskType);
            if (id >= 0) {
                splay.play(id, 1, 1, 1, 0, 1.0f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void release() {
        splay.release();
        splay = null;
    }
}
