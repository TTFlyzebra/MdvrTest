package com.flyzebra.mdvr.screen;

import static android.content.Context.MEDIA_PROJECTION_SERVICE;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.view.Surface;

import com.flyzebra.core.Fzebra;
import com.flyzebra.core.notify.INotify;
import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.NotifyType;
import com.flyzebra.core.notify.Protocol;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenService implements INotify {
    private Context mContext;
    private MediaProjectionManager mpManager = null;
    private int width = 1280;
    private int height = 720;
    private int mBitRate = 4000000;
    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 25; // 30 fps
    private static final int IFRAME_INTERVAL = 5; // 2 seconds between I-frames
    private byte[] video_data = new byte[720 * 1440 * 3 / 2];
    private Thread workThread = null;
    private AtomicBoolean isStop = new AtomicBoolean(true);
    private Hashtable<Long, Long> mScreenUsers = new Hashtable<>();
    private int mResultCode;
    private Intent mResultData;

    private static final HandlerThread mCmdThread = new HandlerThread("screen_cmd");

    static {
        mCmdThread.start();
    }

    private static final Handler mCmdHandler = new Handler(mCmdThread.getLooper());

    public ScreenService(Context context) {
        mContext = context;
    }

    public void start(int resultCode, Intent resultData) {
        mResultCode = resultCode;
        mResultData = resultData;
        mpManager = (MediaProjectionManager) mContext.getSystemService(MEDIA_PROJECTION_SERVICE);
        Notify.get().registerListener(this);
    }

    public void stop() {
        mCmdHandler.removeCallbacksAndMessages(null);
        Notify.get().unregisterListener(this);
    }

    public void screenStart() {
        FlyLog.d("screenStart");
        isStop.set(false);
        workThread = new Thread(() -> {
            FlyLog.d("screen work thread start!");
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            MediaCodec codec = null;
            MediaProjection mMediaProjection = null;
            VirtualDisplay mVirtualDisplay = null;
            try {
                mMediaProjection = mpManager.getMediaProjection(mResultCode, mResultData);
                MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
                format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
                try {
                    codec = MediaCodec.createEncoderByType(MIME_TYPE);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                Surface surface = codec.createInputSurface();
                codec.start();
                mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                        "screen-display",
                        width,
                        height,
                        1,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                        surface,
                        null,
                        null);
                while (!isStop.get()) {
                    Iterator<Map.Entry<Long, Long>> it = mScreenUsers.entrySet().iterator();
                    long crt_time = SystemClock.uptimeMillis();
                    while (it.hasNext()) {
                        Map.Entry<Long, Long> entry = it.next();
                        if (crt_time - entry.getValue() > 10000) {
                            it.remove();
                            FlyLog.d("User timeout disconnect, %d", entry.getKey());
                        }
                    }
                    if (mScreenUsers.isEmpty()) {
                        isStop.set(true);
                        break;
                    }
                    int eobIndex = codec.dequeueOutputBuffer(bufferInfo, 1000);
                    if (isStop.get()) break;
                    if (eobIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat mediaFormat = codec.getOutputFormat();
                        ByteBuffer spsBuffer = mediaFormat.getByteBuffer("csd-0");
                        ByteBuffer ppsBuffer = mediaFormat.getByteBuffer("csd-1");
                        int spsLen = spsBuffer.remaining();
                        int ppsLen = ppsBuffer.remaining();
                        byte[] data = new byte[spsLen + ppsLen];
                        spsBuffer.get(data, 0, spsLen);
                        ppsBuffer.get(data, spsLen, ppsLen);
                        Notify.get().handledata(NotifyType.NOTI_SCREEN_SPS, data, (spsLen + ppsLen), null);
                    } else if (eobIndex >= 0) {
                        ByteBuffer data = codec.getOutputBuffer(eobIndex);
                        data.position(bufferInfo.offset);
                        data.limit(bufferInfo.offset + bufferInfo.size);
                        data.get(video_data, 0, bufferInfo.size);
                        Notify.get().handledata(NotifyType.NOTI_SCREEN_AVC, video_data, bufferInfo.size, null);
                        codec.releaseOutputBuffer(eobIndex, false);
                    }
                }
            } catch (Exception e) {
                FlyLog.e(e.toString());
            } finally {
                if (codec != null) {
                    codec.stop();
                    codec.release();
                }
                if (mVirtualDisplay != null) {
                    mVirtualDisplay.release();
                }
                if (mMediaProjection != null) {
                    mMediaProjection.stop();
                }
            }
            FlyLog.d("screen work thread exit!");
        }, "screen_encoder");
        workThread.start();
    }

    public void screenStop() {
        isStop.set(true);
        try {
            if (workThread != null) {
                workThread.join();
                workThread = null;
            }
        } catch (InterruptedException e) {
            FlyLog.e(e.toString());
        }
        FlyLog.d("screenStop");
    }

    @Override
    public void notify(byte[] data, int size) {
        mCmdHandler.post(() -> handleCmd(data, size));
    }

    @Override
    public void handle(int type, byte[] data, int size, byte[] params) {

    }

    private void handleCmd(byte[] data, int size) {
        short type = ByteUtil.bytes2Short(data, 2, true);
        switch (type) {
            case Protocol.TYPE_UT_HEARTBEAT: {
                long uid = ByteUtil.bytes2Long(data, 16, true);
                mScreenUsers.put(uid, SystemClock.uptimeMillis());
                break;
            }
            case Protocol.TYPE_U_DISCONNECTED: {
                long uid = ByteUtil.bytes2Long(data, 8, true);
                mScreenUsers.remove(uid);
                FlyLog.d("recv screen user is disconnect, client size[%d]", mScreenUsers.size());
                if (mScreenUsers.isEmpty()) screenStop();
                break;
            }
            case Protocol.TYPE_T_DISCONNECTED: {
                mScreenUsers.clear();
                screenStop();
                break;
            }
            case Protocol.TYPE_SCREEN_U_READY: {
                long uid = ByteUtil.bytes2Long(data, 16, true);
                mScreenUsers.put(uid, SystemClock.uptimeMillis());
                Notify.get().miniNotify(Protocol.SCREEN_T_START, Protocol.SCREEN_T_START.length, Fzebra.get().getTid(), 0, null);
                break;
            }
            case Protocol.TYPE_SCREEN_U_START: {
                screenStop();
                screenStart();
                break;
            }
            case Protocol.TYPE_SCREEN_U_STOP: {
                long uid = ByteUtil.bytes2Long(data, 16, true);
                mScreenUsers.remove(uid);
                FlyLog.d("recv screen avc stop, client size[%d]", mScreenUsers.size());
                if (mScreenUsers.isEmpty()) screenStop();
                break;
            }
        }
    }
}
