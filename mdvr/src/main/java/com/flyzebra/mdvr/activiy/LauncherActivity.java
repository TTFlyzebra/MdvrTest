package com.flyzebra.mdvr.activiy;

import android.os.Bundle;
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
    private RadioGroup radioGroup;
    public String[] fragmentName = {"ChannelFrame_0", "ChannelFrame_1", "ChannelFrame_2", "ChannelFrame_3", "ChannelFrame_All"};
    private int cerrent_fragment = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main1);

        PropUtil.set("ctl.start", "MonitorHobotApk");
        ShellUtil.exec("am force-stop com.hobot.sample.app");

        MdvrService.startSelfService(this);

        radioGroup = findViewById(R.id.radio_group);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_btall) {
                replaceFragment(fragmentName[4], R.id.fm_layout);
                cerrent_fragment = 4;
            } else if (checkedId == R.id.radio_bt01) {
                replaceFragment(fragmentName[0], R.id.fm_layout);
                cerrent_fragment = 0;
            } else if (checkedId == R.id.radio_bt02) {
                replaceFragment(fragmentName[1], R.id.fm_layout);
                cerrent_fragment = 1;
            } else if (checkedId == R.id.radio_bt03) {
                replaceFragment(fragmentName[2], R.id.fm_layout);
                cerrent_fragment = 2;
            } else if (checkedId == R.id.radio_bt04) {
                replaceFragment(fragmentName[3], R.id.fm_layout);
                cerrent_fragment = 3;
            }
        });

        replaceFragment(fragmentName[0], R.id.fm_layout);
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

    public void replaceFragment(String classname, int resId) {
        try {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            Class<?> c1 = Class.forName(Objects.requireNonNull(MyApp.class.getPackage()).getName() + ".fm." + classname);
            Fragment fragmentRe = (Fragment) c1.newInstance();
            if (fragmentName[cerrent_fragment].equals(classname)) {
                Class<?> c2 = Class.forName(Objects.requireNonNull(MyApp.class.getPackage()).getName() + ".fm." + fragmentName[cerrent_fragment]);
                Fragment fragmentRm = (Fragment) c2.newInstance();
                transaction.remove(fragmentRm);
            }
            transaction.replace(resId, fragmentRe, classname);
            transaction.commit();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            FlyLog.e(e.toString());
        }
    }
}