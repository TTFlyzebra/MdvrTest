package com.flyzebra.mdvr.fm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.arcsoft.visdrive.sdk.model.adas.ArcADASCalibInfo;
import com.arcsoft.visdrive.sdk.model.adas.ArcADASCalibResult;
import com.flyzebra.mdvr.Config;
import com.flyzebra.mdvr.Global;
import com.flyzebra.mdvr.R;
import com.flyzebra.mdvr.activity.LauncherActivity;
import com.flyzebra.mdvr.arcsoft.AdasService;
import com.flyzebra.mdvr.view.AdasSetView;
import com.flyzebra.utils.FlyLog;
import com.flyzebra.utils.SPUtil;

public class ChannelFrame_adas extends Fragment {
    private final ArcADASCalibInfo calibInfo = new ArcADASCalibInfo();

    private RelativeLayout adas_page1;
    private LinearLayout adas_page2;
    private TextView bt_next_page;
    private TextView bt_front_page;

    private AdasSetView adasSetView;

    private TextView adas_cali_horizon_text;
    private TextView adas_cali_carMiddle_text;
    private TextView adas_cali_horizon_text2;
    private TextView adas_cali_carMiddle_text2;
    private TextView adas_cali_cameraHeight_text;
    private TextView adas_cali_cameraToAxle_text;
    private TextView adas_cali_carWidth_text;
    private TextView adas_cali_cameraToBumper_text;
    private TextView adas_cali_cameraToLeftWheel_text;

    private ImageButton adas_cali_horizont_up;
    private ImageButton adas_cali_horizont_down;
    private ImageButton adas_cali_carMiddle_left;
    private ImageButton adas_cali_carMiddle_right;
    private ImageButton adas_cali_cameraHeight_left;
    private ImageButton adas_cali_cameraHeight_right;
    private ImageButton adas_cali_cameraToAxle_left;
    private ImageButton adas_cali_cameraToAxle_right;
    private ImageButton adas_cali_carWidth_left;
    private ImageButton adas_cali_carWidth_right;
    private ImageButton adas_cali_cameraToBumper_left;
    private ImageButton adas_cali_cameraToBumper_right;
    private ImageButton adas_cali_cameraToLeftWheel_left;
    private ImageButton adas_cali_cameraToLeftWheel_right;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_adas, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        adas_page1 = view.findViewById(R.id.adas_page1);
        adas_page2 = view.findViewById(R.id.adas_page2);
        adas_page1.setVisibility(View.VISIBLE);
        adas_page2.setVisibility(View.INVISIBLE);
        adasSetView = view.findViewById(R.id.adas_set_view);
        adas_cali_horizon_text = view.findViewById(R.id.adas_cali_horizon_text);
        adas_cali_carMiddle_text = view.findViewById(R.id.adas_cali_carMiddle_text);
        adas_cali_horizon_text2 = view.findViewById(R.id.adas_cali_horizon_text2);
        adas_cali_carMiddle_text2 = view.findViewById(R.id.adas_cali_carMiddle_text2);
        adas_cali_cameraHeight_text = view.findViewById(R.id.adas_cali_cameraHeight_text);
        adas_cali_cameraToAxle_text = view.findViewById(R.id.adas_cali_cameraToAxle_text);
        adas_cali_carWidth_text = view.findViewById(R.id.adas_cali_carWidth_text);
        adas_cali_cameraToBumper_text = view.findViewById(R.id.adas_cali_cameraToBumper_text);
        adas_cali_cameraToLeftWheel_text = view.findViewById(R.id.adas_cali_cameraToLeftWheel_text);
        adas_cali_horizont_up = view.findViewById(R.id.adas_cali_horizont_up);
        adas_cali_horizont_down = view.findViewById(R.id.adas_cali_horizont_down);
        adas_cali_carMiddle_left = view.findViewById(R.id.adas_cali_carMiddle_left);
        adas_cali_carMiddle_right = view.findViewById(R.id.adas_cali_carMiddle_right);
        adas_cali_cameraHeight_left = view.findViewById(R.id.adas_cali_cameraHeight_left);
        adas_cali_cameraHeight_right = view.findViewById(R.id.adas_cali_cameraHeight_right);
        adas_cali_cameraToAxle_left = view.findViewById(R.id.adas_cali_cameraToAxle_left);
        adas_cali_cameraToAxle_right = view.findViewById(R.id.adas_cali_cameraToAxle_right);
        adas_cali_carWidth_left = view.findViewById(R.id.adas_cali_carWidth_left);
        adas_cali_carWidth_right = view.findViewById(R.id.adas_cali_carWidth_right);
        adas_cali_cameraToBumper_left = view.findViewById(R.id.adas_cali_cameraToBumper_left);
        adas_cali_cameraToBumper_right = view.findViewById(R.id.adas_cali_cameraToBumper_right);
        adas_cali_cameraToLeftWheel_left = view.findViewById(R.id.adas_cali_cameraToLeftWheel_left);
        adas_cali_cameraToLeftWheel_right = view.findViewById(R.id.adas_cali_cameraToLeftWheel_right);

        calibInfo.horizon = Global.calibInfo.horizon;
        calibInfo.carMiddle = Global.calibInfo.carMiddle;
        calibInfo.cameraToAxle = Global.calibInfo.cameraToAxle;
        calibInfo.cameraToBumper = Global.calibInfo.cameraToBumper;
        calibInfo.cameraHeight = Global.calibInfo.cameraHeight;
        calibInfo.carWidth = Global.calibInfo.carWidth;
        calibInfo.cameraToLeftWheel = Global.calibInfo.cameraToLeftWheel;
        adasSetView.upCalibInfo(calibInfo);

        adasSetView.setMoveLisenter(new AdasSetView.MoveLisenter() {
            @Override
            public void notifyHorizon(int vaule) {
                adas_cali_horizon_text.setText(String.valueOf(calibInfo.horizon));
                String horizon_text = getString(R.string.horizontal_line3) + calibInfo.horizon;
                adas_cali_horizon_text2.setText(horizon_text);
            }

            @Override
            public void notiryCarMiddle(int value) {
                adas_cali_carMiddle_text.setText(String.valueOf(calibInfo.carMiddle));
                String carMiddle_text = getString(R.string.car_central_line3) + calibInfo.carMiddle;
                adas_cali_carMiddle_text2.setText(carMiddle_text);
            }
        });

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
                    activity.radioGroup.check(R.id.radio_bt02);
                }
            } catch (Exception e) {
                FlyLog.e(e.toString());
            }
        });

        TextView bt_adas_cancel = view.findViewById(R.id.bt_adas_cancel);
        bt_adas_cancel.setOnClickListener(v -> {
            LauncherActivity activity = (LauncherActivity) getActivity();
            if (activity != null) {
                activity.radioGroup.check(R.id.radio_bt02);
            }
        });
        bt_next_page = view.findViewById(R.id.bt_next_page);
        bt_next_page.setOnClickListener(v -> {
            //adas_page1.setVisibility(View.INVISIBLE);
            adas_page2.setVisibility(View.VISIBLE);
            bt_next_page.setVisibility(View.GONE);
            bt_front_page.setVisibility(View.VISIBLE);
            adas_cali_horizon_text2.setVisibility(View.GONE);
            adas_cali_carMiddle_text2.setVisibility(View.GONE);
        });
        bt_front_page = view.findViewById(R.id.bt_front_page);
        bt_front_page.setOnClickListener(v -> {
            //adas_page1.setVisibility(View.VISIBLE);
            adas_page2.setVisibility(View.INVISIBLE);
            bt_next_page.setVisibility(View.VISIBLE);
            bt_front_page.setVisibility(View.GONE);
            adas_cali_horizon_text2.setVisibility(View.VISIBLE);
            adas_cali_carMiddle_text2.setVisibility(View.VISIBLE);
        });

        adas_cali_horizon_text.setOnClickListener(v -> showDialog((TextView) v, R.string.horizontal_line2, R.id.adas_cali_horizon_text));
        adas_cali_carMiddle_text.setOnClickListener(v -> showDialog((TextView) v, R.string.car_central_line2, R.id.adas_cali_carMiddle_text));
        adas_cali_cameraHeight_text.setOnClickListener(v -> showDialog((TextView) v, R.string.camera_height, R.id.adas_cali_cameraHeight_text));
        adas_cali_cameraToAxle_text.setOnClickListener(v -> showDialog((TextView) v, R.string.camera_to_axle, R.id.adas_cali_cameraToAxle_text));
        adas_cali_carWidth_text.setOnClickListener(v -> showDialog((TextView) v, R.string.car_width, R.id.adas_cali_carWidth_text));
        adas_cali_cameraToBumper_text.setOnClickListener(v -> showDialog((TextView) v, R.string.camera_to_bumper, R.id.adas_cali_cameraToBumper_text));
        adas_cali_cameraToLeftWheel_text.setOnClickListener(v -> showDialog((TextView) v, R.string.camera_to_left_wheel, R.id.adas_cali_cameraToLeftWheel_text));

        adas_cali_horizont_up.setOnClickListener(v -> {
            if (calibInfo.horizon > 0) {
                calibInfo.horizon--;
                adas_cali_horizon_text.setText(String.valueOf(calibInfo.horizon));
                String horizon_text = getString(R.string.horizontal_line3) + calibInfo.horizon;
                adas_cali_horizon_text2.setText(horizon_text);
                adasSetView.updateHorizonView();
            }
        });
        adas_cali_horizont_down.setOnClickListener(v -> {
            if (calibInfo.horizon < (Config.CAM_HEIGHT - 1)) {
                calibInfo.horizon++;
                adas_cali_horizon_text.setText(String.valueOf(calibInfo.horizon));
                String horizon_text = getString(R.string.horizontal_line3) + calibInfo.horizon;
                adas_cali_horizon_text2.setText(horizon_text);
                adasSetView.updateHorizonView();
            }
        });

        adas_cali_carMiddle_left.setOnClickListener(v -> {
            if (calibInfo.carMiddle > (-Config.CAM_WIDTH / 2) - 1) {
                calibInfo.carMiddle--;
                adas_cali_carMiddle_text.setText(String.valueOf(calibInfo.carMiddle));
                String carMiddle_text = getString(R.string.car_central_line3) + calibInfo.carMiddle;
                adas_cali_carMiddle_text2.setText(carMiddle_text);
                adasSetView.updateCarMiddleView();
            }
        });
        adas_cali_carMiddle_right.setOnClickListener(v -> {
            if (calibInfo.carMiddle < (Config.CAM_WIDTH) / 2 - 1) {
                calibInfo.carMiddle++;
                adas_cali_carMiddle_text.setText(String.valueOf(calibInfo.carMiddle));
                String carMiddle_text = getString(R.string.car_central_line3) + calibInfo.carMiddle;
                adas_cali_carMiddle_text2.setText(carMiddle_text);
                adasSetView.updateCarMiddleView();
            }
        });

        adas_cali_cameraHeight_left.setOnClickListener(v -> {
            if (calibInfo.cameraHeight > 0) {
                calibInfo.cameraHeight--;
                adas_cali_cameraHeight_text.setText(String.valueOf(calibInfo.cameraHeight));
            }
        });
        adas_cali_cameraHeight_right.setOnClickListener(v -> {
            calibInfo.cameraHeight++;
            adas_cali_cameraHeight_text.setText(String.valueOf(calibInfo.cameraHeight));
        });

        adas_cali_cameraToAxle_left.setOnClickListener(v -> {
            calibInfo.cameraToAxle--;
            adas_cali_cameraToAxle_text.setText(String.valueOf(calibInfo.cameraToAxle));
        });
        adas_cali_cameraToAxle_right.setOnClickListener(v -> {
            calibInfo.cameraToAxle++;
            adas_cali_cameraToAxle_text.setText(String.valueOf(calibInfo.cameraToAxle));
        });

        adas_cali_carWidth_left.setOnClickListener(v -> {
            if (calibInfo.carWidth > 0) {
                calibInfo.carWidth--;
                adas_cali_carWidth_text.setText(String.valueOf(calibInfo.carWidth));
            }
        });
        adas_cali_carWidth_right.setOnClickListener(v -> {
            calibInfo.carWidth++;
            adas_cali_carWidth_text.setText(String.valueOf(calibInfo.carWidth));
        });

        adas_cali_cameraToBumper_left.setOnClickListener(v -> {
            calibInfo.cameraToBumper--;
            adas_cali_cameraToBumper_text.setText(String.valueOf(calibInfo.cameraToBumper));
        });
        adas_cali_cameraToBumper_right.setOnClickListener(v -> {
            calibInfo.cameraToBumper++;
            adas_cali_cameraToBumper_text.setText(String.valueOf(calibInfo.cameraToBumper));
        });

        adas_cali_cameraToLeftWheel_left.setOnClickListener(v -> {
            calibInfo.cameraToLeftWheel--;
            adas_cali_cameraToLeftWheel_text.setText(String.valueOf(calibInfo.cameraToLeftWheel));
        });
        adas_cali_cameraToLeftWheel_right.setOnClickListener(v -> {
            calibInfo.cameraToLeftWheel++;
            adas_cali_cameraToLeftWheel_text.setText(String.valueOf(calibInfo.cameraToLeftWheel));
        });

        updateView();
    }

    private void showDialog(TextView textView, int textId, int resID) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_adsa_edit, null);
        TextView title = view.findViewById(R.id.adas_dlg_text);
        EditText edit = view.findViewById(R.id.adas_dlg_edit);
        edit.setText(textView.getText());
        edit.requestFocus();
        Button bt1 = view.findViewById(R.id.lg_dlg_bt1);
        title.setText(textId);
        final AlertDialog dlg = new AlertDialog.Builder(getActivity(), R.style.ThemeFull).setView(view).show();
        bt1.setOnClickListener(v -> {
            textView.setText(edit.getText());
            if (resID == R.id.adas_cali_horizon_text) {
                calibInfo.horizon = Integer.parseInt(edit.getText().toString());
                adasSetView.updateHorizonView();
            } else if (resID == R.id.adas_cali_carMiddle_text) {
                calibInfo.carMiddle = Integer.parseInt(edit.getText().toString());
                adasSetView.updateCarMiddleView();
            } else if (resID == R.id.adas_cali_cameraHeight_text) {
                calibInfo.cameraHeight = Integer.parseInt(edit.getText().toString());
            } else if (resID == R.id.adas_cali_cameraToAxle_text) {
                calibInfo.cameraToAxle = Integer.parseInt(edit.getText().toString());
            } else if (resID == R.id.adas_cali_carWidth_text) {
                calibInfo.carWidth = Integer.parseInt(edit.getText().toString());
            } else if (resID == R.id.adas_cali_cameraToBumper_text) {
                calibInfo.cameraToBumper = Integer.parseInt(edit.getText().toString());
            } else if (resID == R.id.adas_cali_cameraToLeftWheel_text) {
                calibInfo.cameraToLeftWheel = Integer.parseInt(edit.getText().toString());
            }
            dlg.dismiss();
        });
    }

    private void updateView() {
        adas_cali_horizon_text.setText(String.valueOf(calibInfo.horizon));
        adas_cali_carMiddle_text.setText(String.valueOf(calibInfo.carMiddle));
        String horizon_text = getString(R.string.horizontal_line3) + calibInfo.horizon;
        adas_cali_horizon_text2.setText(horizon_text);
        String carMiddle_text = getString(R.string.car_central_line3) + calibInfo.carMiddle;
        adas_cali_carMiddle_text2.setText(carMiddle_text);
        adas_cali_cameraHeight_text.setText(String.valueOf(calibInfo.cameraHeight));
        adas_cali_cameraToAxle_text.setText(String.valueOf(calibInfo.cameraToAxle));
        adas_cali_carWidth_text.setText(String.valueOf(calibInfo.carWidth));
        adas_cali_cameraToBumper_text.setText(String.valueOf(calibInfo.cameraToBumper));
        adas_cali_cameraToLeftWheel_text.setText(String.valueOf(calibInfo.cameraToLeftWheel));
        adasSetView.upCalibInfo(calibInfo);
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
}
