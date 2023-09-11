package com.flyzebra.mdvr.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.flyzebra.core.Fzebra;
import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.Protocol;
import com.flyzebra.mdvr.MdvrService;
import com.flyzebra.mdvr.MyApp;
import com.flyzebra.mdvr.R;
import com.flyzebra.utils.FlyLog;
import com.flyzebra.utils.PropUtil;
import com.flyzebra.utils.ShellUtil;

import java.util.Objects;

public class LauncherActivity extends AppCompatActivity {
    public RadioGroup radioGroup;
    private String current_fm1 = "";
    private String current_fm2 = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_mdvr);

        PropUtil.set("ctl.start", "MonitorHobotApk");
        ShellUtil.exec("am force-stop com.hobot.sample.app");

        MdvrService.startSelfService(this);

        radioGroup = findViewById(R.id.radio_group);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_bt01) {
                replaceFragment1("ChannelFrame_0");
                replaceFragment2("ChannelFrame_empty");
            } else if (checkedId == R.id.radio_bt02) {
                replaceFragment1("ChannelFrame_1");
                replaceFragment2("ChannelFrame_empty");
            } else if (checkedId == R.id.radio_bt03) {
                replaceFragment1("ChannelFrame_2");
                replaceFragment2("ChannelFrame_empty");
            } else if (checkedId == R.id.radio_bt04) {
                replaceFragment1("ChannelFrame_3");
                replaceFragment2("ChannelFrame_empty");
            } else if (checkedId == R.id.radio_btall) {
                replaceFragment1("ChannelFrame_all");
                replaceFragment2("ChannelFrame_empty");
            } else if (checkedId == R.id.radio_adas) {
                replaceFragment1("ChannelFrame_1");
                replaceFragment2("ChannelFrame_adas");
                radioGroup.setVisibility(View.INVISIBLE);
            }
        });

        replaceFragment1("ChannelFrame_0");
        replaceFragment2("ChannelFrame_empty");
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
    public void onBackPressed() {
        super.onBackPressed();
        Notify.get().miniNotify(Protocol.HOME_BACK, Protocol.HOME_BACK.length, Fzebra.get().getTid(), 0, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unregisterReceiver(recevier);
        //stopService(new Intent(this, MdvrService.class));
    }

    public void replaceFragment1(String classname) {
        try {
            if (TextUtils.isEmpty(classname) || current_fm1.equals(classname)) return;
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            Class<?> c1 = Class.forName(Objects.requireNonNull(MyApp.class.getPackage()).getName() + ".fm." + classname);
            Fragment fragmentRe = (Fragment) c1.newInstance();
            transaction.replace(R.id.fm_layout1, fragmentRe, classname);
            transaction.commit();
            current_fm1 = classname;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            FlyLog.e(e.toString());
        }
    }

    public void replaceFragment2(String classname) {
        try {
            if (TextUtils.isEmpty(classname) || current_fm2.equals(classname)) return;
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            Class<?> c1 = Class.forName(Objects.requireNonNull(MyApp.class.getPackage()).getName() + ".fm." + classname);
            Fragment fragmentRe = (Fragment) c1.newInstance();
            transaction.replace(R.id.fm_layout2, fragmentRe, classname);
            transaction.commit();
            current_fm2 = classname;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            FlyLog.e(e.toString());
        }
    }
}