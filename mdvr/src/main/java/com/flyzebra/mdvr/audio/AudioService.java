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
public class AudioService {
    private Context mContext;
    private int sampleRate;
    private int channelCount;
    private int audioFormat;
    private AudioRecord mAudioRecord;
    private AtomicBoolean is_stop = new AtomicBoolean(true);
    private byte[] pcm = new byte[1024];
    private Thread mRecordThread = null;

    public AudioService(Context context) {
        this.mContext = context;
    }

    public void onCreate(int sample, int channel, int format) {
        this.sampleRate = sample;
        this.channelCount = channel;
        this.audioFormat = format;
        int bufferSize = AudioRecord.getMinBufferSize(sample, channel, format);
        if (ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            FlyLog.e("check audio record permission failed!");
            return;
        }
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER, sample, channel, format, bufferSize);
        mRecordThread = new Thread(() -> {
            mAudioRecord.startRecording();
            while (!is_stop.get()) {
                int readSize = mAudioRecord.read(pcm, 0, pcm.length);
                if (readSize > 0) {
                    byte[] pcm_0 = new byte[readSize / 2];
                    byte[] pcm_1 = new byte[readSize / 2];
                    byte[] pcm_2 = new byte[readSize / 2];
                    byte[] pcm_3 = new byte[readSize / 2];
                    for (int i = 0; i < readSize / 4; i++) {
                        pcm_0[i * 2 + 1] = pcm[i * 4 + 1];
                        pcm_1[i * 2 + 1] = pcm[i * 4];
                        pcm_2[i * 2 + 1] = pcm[i * 4 + 3];
                        pcm_3[i * 2 + 1] = pcm[i * 4 + 2];
                    }

                    byte[] params = new byte[12];
                    ByteUtil.intToBytes(sampleRate, params, 2, true);
                    ByteUtil.shortToBytes((short) Config.MIC_CHANNELS, params, 6, true);
                    ByteUtil.intToBytes(Config.MIC_BIT_RATE, params, 8, true);

                    ByteUtil.shortToBytes((short) 0, params, 0, true);
                    Notify.get().handledata(NotifyType.NOTI_MICOUT_PCM, pcm_0, readSize / 2, params);
                    ByteUtil.shortToBytes((short) 1, params, 0, true);
                    Notify.get().handledata(NotifyType.NOTI_MICOUT_PCM, pcm_1, readSize / 2, params);
                    ByteUtil.shortToBytes((short) 2, params, 0, true);
                    Notify.get().handledata(NotifyType.NOTI_MICOUT_PCM, pcm_2, readSize / 2, params);
                    ByteUtil.shortToBytes((short) 3, params, 0, true);
                    Notify.get().handledata(NotifyType.NOTI_MICOUT_PCM, pcm_3, readSize / 2, params);
                }else{
                    FlyLog.e("Audio read mic buffer error, readSize=%d", readSize);
                }
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
    }
}
