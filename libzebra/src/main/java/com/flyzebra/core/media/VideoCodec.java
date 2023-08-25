/**
 * FileName: VideoEncoder
 * Author: FlyZebra
 * Email:flycnzebra@gmail.com
 * Date: 2023/6/23 15:06
 * Description:
 */
package com.flyzebra.core.media;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import com.flyzebra.utils.FlyLog;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class VideoCodec implements Runnable {
    private final int mChannel;
    private MediaCodec codec = null;
    private final AtomicBoolean is_codec_init = new AtomicBoolean(false);
    private Thread mOutThread = null;
    private final VideoCodecCB mCallBack;

    public VideoCodec(int channel, VideoCodecCB cb) {
        mChannel = channel;
        mCallBack = cb;
    }

    public void initCodec(String mimeType, int width, int height, int fps, int i_frame, int bitrate, int bitrate_mode) {
        try {
            MediaFormat format = MediaFormat.createVideoFormat(mimeType, width, height);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, i_frame);
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate * 1024);
            format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
            format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileMain);
            format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel22);
            codec = MediaCodec.createEncoderByType(mimeType);
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            codec.start();
            is_codec_init.set(true);
            mOutThread = new Thread(this, "v-encoder" + mChannel);
            mOutThread.start();
        } catch (Exception e) {
            FlyLog.e(e.toString());
        }
    }

    public void inYuvData(byte[] data, int size, long pts) {
        if (!is_codec_init.get() || data == null || size <= 0) return;
        int inIndex = codec.dequeueInputBuffer(200000);
        if (inIndex < 0) {
            FlyLog.e("VideoEncoder codec->dequeueInputBuffer inIdex=%d error!", inIndex);
            return;
        }

        ByteBuffer buffer = codec.getInputBuffer(inIndex);
        if (buffer == null) {
            FlyLog.e("VideoEncoder codec->getInputBuffer inIdex=%d error!", inIndex);
            return;
        }
        buffer.put(data, 0, size);
        codec.queueInputBuffer(inIndex, 0, size, pts, 0);
    }

    public void releaseCodec() {
        is_codec_init.set(false);
        try {
            mOutThread.join();
        } catch (InterruptedException e) {
            FlyLog.e(e.toString());
        }
        codec.stop();
        codec.release();
        codec = null;
    }

    @Override
    public void run() {
        MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
        while (is_codec_init.get()) {
            int outputIndex = codec.dequeueOutputBuffer(mBufferInfo, 200000);
            switch (outputIndex) {
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    try {
                        MediaFormat format = codec.getOutputFormat();
                        String mini = format.getString(MediaFormat.KEY_MIME);
                        if (MediaFormat.MIMETYPE_VIDEO_AVC.equals(mini)) {
                            ByteBuffer spsBuffer = format.getByteBuffer("csd-0");
                            spsBuffer.position(0);
                            int spsLen = spsBuffer.remaining();
                            ByteBuffer ppsBuffer = format.getByteBuffer("csd-1");
                            ppsBuffer.position(0);
                            int ppsLen = ppsBuffer.remaining();
                            byte[] data = new byte[spsLen + ppsLen];
                            spsBuffer.get(data, 0, spsLen);
                            ppsBuffer.get(data, spsLen, ppsLen);
                            mCallBack.notifySpsPps(mChannel, data, spsLen + ppsLen);
                        } else {
                            ByteBuffer bufer = format.getByteBuffer("csd-0");
                            bufer.position(0);
                            int size = bufer.remaining();
                            byte[] data = new byte[size];
                            bufer.get(data, 0, size);
                            mCallBack.notifyVpsSpsPps(mChannel, data, size);
                        }
                    } catch (Exception e) {
                        FlyLog.e(e.toString());
                    }
                    break;
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    break;
                default:
                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && mBufferInfo.size != 0) {
                        ByteBuffer outputBuffer = codec.getOutputBuffer(outputIndex);
                        outputBuffer.position(mBufferInfo.offset + 4);
                        int size = outputBuffer.remaining();
                        byte[] data = new byte[size];
                        outputBuffer.get(data, 0, size);
                        //long pts = System.nanoTime()/1000L;
                        MediaFormat format = codec.getOutputFormat();
                        String mini = format.getString(MediaFormat.KEY_MIME);
                        if (MediaFormat.MIMETYPE_VIDEO_AVC.equals(mini)) {
                            mCallBack.notifyAvcData(mChannel, data, size, mBufferInfo.presentationTimeUs);
                        } else {
                            mCallBack.notifyHevcData(mChannel, data, size, mBufferInfo.presentationTimeUs);
                        }
                    }
                    codec.releaseOutputBuffer(outputIndex, false);
                    break;
            }
        }
    }
}
