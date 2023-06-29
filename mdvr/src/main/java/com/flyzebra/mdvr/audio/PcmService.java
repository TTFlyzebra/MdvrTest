package com.flyzebra.mdvr.audio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import androidx.core.app.ActivityCompat;

import com.flyzebra.mdvr.Config;
import com.flyzebra.notify.Notify;
import com.flyzebra.notify.NotifyType;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * rn9175音频，四路麦克风使用一路48k2channels16bit录入，每路为48k1channel8bit
 * 实际每路转为48k1channel16bit
 */
public class PcmService {
    private final Context mContext;
    private int sampleRate;
    private AudioRecord mAudioRecord;
    private final AtomicBoolean is_stop = new AtomicBoolean(true);
    int pcmSize = (int) (Config.MIC_SAMPLE * 1.0f * 16 / 8 * 2 / 25.0f);
    private final byte[] pcm = new byte[pcmSize];
    private Thread mRecordThread = null;

    public PcmService(Context context) {
        this.mContext = context;
    }

    public void onCreate(int sample, int channel, int format) {
        FlyLog.d("PcmService start!");
        this.sampleRate = sample;
        int bufferSize = AudioRecord.getMinBufferSize(sample, channel, format);
        if (ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            FlyLog.e("check audio record permission failed!");
            return;
        }
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER, sample, channel, format, bufferSize);
        mRecordThread = new Thread(() -> {
            mAudioRecord.startRecording();
            int cPcmSize = pcmSize;
            byte[] pcm_0 = new byte[cPcmSize];
            byte[] pcm_1 = new byte[cPcmSize];
            byte[] pcm_2 = new byte[cPcmSize];
            byte[] pcm_3 = new byte[cPcmSize];
            while (!is_stop.get()) {
                int readSize = 0;
                while (!is_stop.get() && readSize < pcmSize) {
                    int readLen = mAudioRecord.read(pcm, readSize, pcmSize - readSize);
                    readSize += readLen;
                }
                if (is_stop.get()) break;

                for (int i = 0; i < readSize / 4; i++) {
                    pcm_0[i * 4 + 3] = pcm[i * 4 + 1];
                    pcm_1[i * 4 + 3] = pcm[i * 4];
                    pcm_2[i * 4 + 3] = pcm[i * 4 + 3];
                    pcm_3[i * 4 + 3] = pcm[i * 4 + 2];
                }

                byte[] params = new byte[20];
                ByteUtil.intToBytes(sampleRate, params, 2, true);
                ByteUtil.shortToBytes((short) Config.MIC_CHANNELS, params, 6, true);
                ByteUtil.intToBytes(Config.MIC_BIT_RATE, params, 8, true);
                ByteUtil.longToBytes(System.nanoTime() / 1000, params, 12, true);

                ByteUtil.shortToBytes((short) 0, params, 0, true);
                Notify.get().handledata(NotifyType.NOTI_MICOUT_PCM, pcm_0, cPcmSize, params);
                ByteUtil.shortToBytes((short) 1, params, 0, true);
                Notify.get().handledata(NotifyType.NOTI_MICOUT_PCM, pcm_1, cPcmSize, params);
                ByteUtil.shortToBytes((short) 2, params, 0, true);
                Notify.get().handledata(NotifyType.NOTI_MICOUT_PCM, pcm_2, cPcmSize, params);
                ByteUtil.shortToBytes((short) 3, params, 0, true);
                Notify.get().handledata(NotifyType.NOTI_MICOUT_PCM, pcm_3, cPcmSize, params);
            }
            mAudioRecord.stop();
        });

        is_stop.set(false);
        mRecordThread.start();
    }

    public void onDistory() {
        is_stop.set(true);
        try {
            mRecordThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        FlyLog.d("PcmService exit!");
    }
}
