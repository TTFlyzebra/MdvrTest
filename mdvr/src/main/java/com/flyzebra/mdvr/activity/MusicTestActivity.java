package com.flyzebra.mdvr.activity;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.arcsoft.visdrive.sdk.constant.adas.ArcADASDetectMaskType;
import com.arcsoft.visdrive.sdk.constant.dms.ArcDMSDetectMaskType;
import com.flyzebra.mdvr.R;
import com.flyzebra.mdvr.arcsoft.AlertMusicPlayer;

public class MusicTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_test);

        AlertMusicPlayer.get().loadAdasMusic(this);
        AlertMusicPlayer.get().loadDmsMusic(this);
    }

    public void mod_dms_call(View view) {
        AlertMusicPlayer.get().playDms(ArcDMSDetectMaskType.MOD_DMS_CALL);
    }

    public void mod_dms_smoke(View view) {
        AlertMusicPlayer.get().playDms(ArcDMSDetectMaskType.MOD_DMS_SMOKE);
    }

    public void mod_dms_close_eye(View view) {
        AlertMusicPlayer.get().playDms(ArcDMSDetectMaskType.MOD_DMS_CLOSE_EYE);
    }

    public void mod_dms_yawn(View view) {
        AlertMusicPlayer.get().playDms(ArcDMSDetectMaskType.MOD_DMS_YAWN);
    }

    public void mod_dms_distract(View view) {
        AlertMusicPlayer.get().playDms(ArcDMSDetectMaskType.MOD_DMS_DISTRACT);
    }

    public void mod_dms_driver_abnormal(View view) {
        AlertMusicPlayer.get().playDms(ArcDMSDetectMaskType.MOD_DMS_DRIVER_ABNORMAL);
    }

    public void mod_dms_ir_blocking(View view) {
        AlertMusicPlayer.get().playDms(ArcDMSDetectMaskType.MOD_DMS_IR_BLOCKING);
    }

    public void mod_dms_lens_covered(View view) {
        AlertMusicPlayer.get().playDms(ArcDMSDetectMaskType.MOD_DMS_LENS_COVERED);
    }

    public void mod_dms_seatbelt_unfastened(View view) {
        AlertMusicPlayer.get().playDms(ArcDMSDetectMaskType.MOD_DMS_SEATBELT_UNFASTENED);
    }

    public void mod_adas_ldw(View view) {
        AlertMusicPlayer.get().playAdas(ArcADASDetectMaskType.MOD_ADAS_LDW);
    }

    public void mod_adas_fcw(View view) {
        AlertMusicPlayer.get().playAdas(ArcADASDetectMaskType.MOD_ADAS_FCW);
    }

    public void mod_adas_hmw(View view) {
        AlertMusicPlayer.get().playAdas(ArcADASDetectMaskType.MOD_ADAS_HMW);
    }

    public void mod_adas_pcw(View view) {
        AlertMusicPlayer.get().playAdas(ArcADASDetectMaskType.MOD_ADAS_PCW);
    }

}