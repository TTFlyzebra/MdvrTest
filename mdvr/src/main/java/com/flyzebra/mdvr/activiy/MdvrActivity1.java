package com.flyzebra.mdvr.activiy;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.flyzebra.core.notify.INotify;
import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.NotifyType;
import com.flyzebra.mdvr.Config;
import com.flyzebra.mdvr.MdvrService;
import com.flyzebra.mdvr.R;
import com.flyzebra.mdvr.view.GlVideoView;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;
import com.flyzebra.utils.PropUtil;
import com.flyzebra.utils.SPUtil;
import com.flyzebra.utils.ShellUtil;

public class MdvrActivity1 extends AppCompatActivity implements INotify {
    //private MyRecevier recevier = new MyRecevier();
    private final GlVideoView[] mGlVideoViews = new GlVideoView[Config.MAX_CAM];
    private final int[] mGlVideoViewIds = new int[]{R.id.sv01, R.id.sv02, R.id.sv03, R.id.sv04};
    private GlVideoView glVideoView;
    private LinearLayout layoutSurfaceViews;
    private RadioGroup radioGroup;
    private int mSelectChannel = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FlyLog.d("MdvrActiviy start!");

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main1);

        PropUtil.set("ctl.stop", "MonitorHobotApk");
        ShellUtil.exec("am force-stop com.hobot.sample.app");

        startService(new Intent(this, MdvrService.class));
        Notify.get().registerListener(this);

        glVideoView = findViewById(R.id.full_sv);
        layoutSurfaceViews = findViewById(R.id.ll_svs);

        for (int i = 0; i < Config.MAX_CAM; i++) {
            mGlVideoViews[i] = findViewById(mGlVideoViewIds[i]);
        }

        radioGroup = findViewById(R.id.radio_group);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radio_btall) {
                    FlyLog.e("setOnCheckedChangeListener all selected!");
                    mSelectChannel = -1;
                } else if (checkedId == R.id.radio_bt01) {
                    mSelectChannel = 0;
                } else if (checkedId == R.id.radio_bt02) {
                    mSelectChannel = 1;
                } else if (checkedId == R.id.radio_bt03) {
                    mSelectChannel = 2;
                } else if (checkedId == R.id.radio_bt04) {
                    mSelectChannel = 3;
                }
                SPUtil.set(MdvrActivity1.this, "SELECET_CHANNEL", mSelectChannel);
                glVideoView.setVisibility(mSelectChannel == -1 ? View.GONE : View.VISIBLE);
                layoutSurfaceViews.setVisibility(mSelectChannel == -1 ? View.VISIBLE : View.GONE);
            }
        });

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.hardware.usb.action.USB_STATE");
        //registerReceiver(recevier, intentFilter);

        mSelectChannel = (int) SPUtil.get(MdvrActivity1.this, "SELECET_CHANNEL", 0);
        if (mSelectChannel == -1) {
            ((RadioButton) findViewById(R.id.radio_btall)).setChecked(true);
        } else if (mSelectChannel == 0) {
            ((RadioButton) findViewById(R.id.radio_bt01)).setChecked(true);
        } else if (mSelectChannel == 1) {
            ((RadioButton) findViewById(R.id.radio_bt02)).setChecked(true);
        } else if (mSelectChannel == 2) {
            ((RadioButton) findViewById(R.id.radio_bt03)).setChecked(true);
        } else if (mSelectChannel == 3) {
            ((RadioButton) findViewById(R.id.radio_bt04)).setChecked(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unregisterReceiver(recevier);
        Notify.get().unregisterListener(this);
        stopService(new Intent(this, MdvrService.class));
        FlyLog.d("MdvrActiviy exit!");
    }

    @Override
    public void notify(byte[] data, int size) {

    }

    @Override
    public void handle(int type, byte[] data, int size, byte[] params) {
        if (type == NotifyType.NOTI_CAMOUT_YUV) {
            int channel = ByteUtil.bytes2Short(params, 0, true);
            int width = ByteUtil.bytes2Short(params, 2, true);
            int height = ByteUtil.bytes2Short(params, 4, true);
            if (mSelectChannel == -1) {
                mGlVideoViews[channel].upFrame(data, size, width, height);
            } else if (mSelectChannel == channel) {
                glVideoView.upFrame(data, size, width, height);
            }
        }
    }
}