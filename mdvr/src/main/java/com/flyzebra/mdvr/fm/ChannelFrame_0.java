package com.flyzebra.mdvr.fm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.flyzebra.core.notify.INotify;
import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.NotifyType;
import com.flyzebra.mdvr.Global;
import com.flyzebra.mdvr.R;
import com.flyzebra.mdvr.view.AdasSetView;
import com.flyzebra.mdvr.view.GlVideoView;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

public class ChannelFrame_0 extends Fragment  implements INotify, AdasSetView.MoveLisenter {

    private GlVideoView glVideoView;
    private AdasSetView adasSetView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_channel0, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        glVideoView = view.findViewById(R.id.full_sv);
        adasSetView = view.findViewById(R.id.adas_set_view);
        adasSetView.setMoveLisenter(this);

        adasSetView.upCalibInfo(Global.calibInfo);
    }

    @Override
    public void onStart() {
        super.onStart();
        Notify.get().registerListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        Notify.get().unregisterListener(this);
    }

    @Override
    public void notify(byte[] data, int size) {

    }

    @Override
    public void handle(int type, byte[] data, int dsize, byte[] params, int psize) {
        if (type == NotifyType.NOTI_CAMOUT_YUV) {
            int channel = ByteUtil.bytes2Short(params, 0, true);
            int width = ByteUtil.bytes2Short(params, 2, true);
            int height = ByteUtil.bytes2Short(params, 4, true);
            if (channel == 0) {
                glVideoView.upFrame(data, dsize, width, height);
            }
        }
    }

    @Override
    public void notifyHorizon(int horizo) {
        FlyLog.d("notifyHorizon %d", horizo);
    }

    @Override
    public void notiryCarMiddle(int carMiddle) {
        FlyLog.d("notiryCarMiddle %d", carMiddle);
    }
}
