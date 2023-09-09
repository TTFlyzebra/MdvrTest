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
import com.flyzebra.mdvr.Config;
import com.flyzebra.mdvr.R;
import com.flyzebra.mdvr.view.GlVideoView;
import com.flyzebra.utils.ByteUtil;

public class ChannelFrame_all extends Fragment implements INotify {
    private final GlVideoView[] mGlVideoViews = new GlVideoView[Config.MAX_CAM];
    private final int[] mGlVideoViewIds = new int[]{R.id.sv01, R.id.sv02, R.id.sv03, R.id.sv04};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_channelall, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        for (int i = 0; i < Config.MAX_CAM; i++) {
            mGlVideoViews[i] = view.findViewById(mGlVideoViewIds[i]);
        }
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
            mGlVideoViews[channel].upFrame(data, dsize, width, height);
        }
    }
}
