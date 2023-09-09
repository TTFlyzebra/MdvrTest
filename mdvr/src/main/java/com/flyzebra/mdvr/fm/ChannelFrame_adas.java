package com.flyzebra.mdvr.fm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.arcsoft.visdrive.sdk.model.adas.ArcADASCalibInfo;
import com.arcsoft.visdrive.sdk.model.adas.ArcADASCalibResult;
import com.flyzebra.mdvr.Global;
import com.flyzebra.mdvr.R;
import com.flyzebra.mdvr.activity.LauncherActivity;
import com.flyzebra.mdvr.arcsoft.AdasService;
import com.flyzebra.mdvr.view.AdasSetView;
import com.flyzebra.utils.FlyLog;
import com.flyzebra.utils.SPUtil;

public class ChannelFrame_adas extends Fragment implements AdasSetView.MoveLisenter {
    private final ArcADASCalibInfo calibInfo = new ArcADASCalibInfo();

    private RelativeLayout adas_page1;
    private RelativeLayout adas_page2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_adas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adas_page1 = view.findViewById(R.id.adas_page1);
        adas_page2 = view.findViewById(R.id.adas_page2);

        adas_page1.setVisibility(View.VISIBLE);
        adas_page2.setVisibility(View.INVISIBLE);

        AdasSetView adasSetView = view.findViewById(R.id.adas_set_view);
        adasSetView.setMoveLisenter(this);
        calibInfo.horizon = Global.calibInfo.horizon;
        calibInfo.carMiddle = Global.calibInfo.carMiddle;
        calibInfo.cameraToAxle = Global.calibInfo.cameraToAxle;
        calibInfo.cameraToBumper = Global.calibInfo.cameraToBumper;
        calibInfo.cameraHeight = Global.calibInfo.cameraHeight;
        calibInfo.carWidth = Global.calibInfo.carWidth;
        calibInfo.cameraToLeftWheel = Global.calibInfo.cameraToLeftWheel;
        adasSetView.upCalibInfo(calibInfo);

        TextView bt_adas_save = view.findViewById(R.id.bt_adas_save);
        bt_adas_save.setOnClickListener(v -> {
            try {
                ArcADASCalibResult calibResult = AdasService.get().setCalibInfo(calibInfo);
                if (calibResult != null) {
                    Global.calibInfo.horizon = calibInfo.horizon;
                    Global.calibInfo.carMiddle = calibInfo.carMiddle;
                    Global.calibInfo.cameraToAxle = calibInfo.cameraToAxle;
                    Global.calibInfo.cameraToBumper = calibInfo.cameraToBumper;
                    Global.calibInfo.cameraHeight = calibInfo.cameraHeight;
                    Global.calibInfo.carWidth = calibInfo.carWidth;
                    Global.calibInfo.cameraToLeftWheel = calibInfo.cameraToLeftWheel;

                    SPUtil.set(getActivity(), "calibInfo_horizon", calibInfo.horizon);
                    SPUtil.set(getActivity(), "calibInfo_carMiddle", calibInfo.carMiddle);
                    SPUtil.set(getActivity(), "calibInfo_cameraToAxle", calibInfo.cameraToAxle);
                    SPUtil.set(getActivity(), "calibInfo_cameraToBumper", calibInfo.cameraToBumper);
                    SPUtil.set(getActivity(), "calibInfo_cameraHeight", calibInfo.cameraHeight);
                    SPUtil.set(getActivity(), "calibInfo_carWidth", calibInfo.carWidth);
                    SPUtil.set(getActivity(), "calibInfo_cameraToLeftWheel", calibInfo.cameraToLeftWheel);

                    Global.calibResult = calibResult;
                    SPUtil.set(getActivity(), "calibResult_r1", String.valueOf(calibResult.r1));
                    SPUtil.set(getActivity(), "calibResult_r2", String.valueOf(calibResult.r2));
                    SPUtil.set(getActivity(), "calibResult_r3", String.valueOf(calibResult.r3));
                    SPUtil.set(getActivity(), "calibResult_t1", String.valueOf(calibResult.t1));
                    SPUtil.set(getActivity(), "calibResult_t2", String.valueOf(calibResult.t2));
                    SPUtil.set(getActivity(), "calibResult_t3", String.valueOf(calibResult.t3));
                    SPUtil.set(getActivity(), "calibResult_pitch", String.valueOf(calibResult.pitch));
                    SPUtil.set(getActivity(), "calibResult_yaw", String.valueOf(calibResult.yaw));
                    SPUtil.set(getActivity(), "calibResult_roll", String.valueOf(calibResult.roll));
                }

                LauncherActivity activity = (LauncherActivity) getActivity();
                if (activity != null) {
                    activity.radioGroup.check(R.id.radio_bt01);
                }
            } catch (Exception e) {
                FlyLog.e(e.toString());
            }
        });

        TextView bt_next_page = view.findViewById(R.id.bt_next_page);
        bt_next_page.setOnClickListener(v -> {
            adas_page1.setVisibility(View.INVISIBLE);
            adas_page2.setVisibility(View.VISIBLE);
        });
        TextView bt_front_page = view.findViewById(R.id.bt_front_page);
        bt_front_page.setOnClickListener(v -> {
            adas_page1.setVisibility(View.VISIBLE);
            adas_page2.setVisibility(View.INVISIBLE);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        LauncherActivity activity = (LauncherActivity) getActivity();
        if (activity != null) {
            activity.radioGroup.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        LauncherActivity activity = (LauncherActivity) getActivity();
        if (activity != null) {
            activity.radioGroup.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void notifyHorizon(int horizo) {
        calibInfo.horizon = horizo;
    }

    @Override
    public void notiryCarMiddle(int carMiddle) {
        calibInfo.carMiddle = carMiddle;
    }
}
