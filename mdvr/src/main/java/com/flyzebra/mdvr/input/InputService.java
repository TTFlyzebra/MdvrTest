package com.flyzebra.mdvr.input;

import android.content.Context;
import android.graphics.Point;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.flyzebra.core.notify.INotify;
import com.flyzebra.core.notify.Notify;
import com.flyzebra.core.notify.Protocol;
import com.flyzebra.mdvr.input.wrappers.ServiceManager;
import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class InputService implements INotify {
    private final Context mContext;
    private final ByteBuffer eventBuf = ByteBuffer.allocate(1024);
    private final Object eventLock = new Object();
    private int screen_w;
    private int screen_h;
    private final AtomicBoolean is_stop = new AtomicBoolean(true);
    private long verityTime;
    private long lastDownTime;
    private static final int MAX_POINTERS = 8;
    private final MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[MAX_POINTERS];
    private final MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[MAX_POINTERS];
    private PowerManager powerManager = null;
    private static final byte[] INPUT_KEYCODE_POWER = {
            (byte) 0xEE, (byte) 0xAA, (byte) 0x03, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1A, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
    private Thread eventThread = null;
    private final ServiceManager serviceManager = new ServiceManager();

    public InputService(Context context) {
        mContext = context;
    }

    public void start() {
        FlyLog.d("InputService start!");
        powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Point screen = new Point();
        windowManager.getDefaultDisplay().getRealSize(screen);
        screen_w = screen.x;
        screen_h = screen.y;
        for (int i = 0; i < MAX_POINTERS; ++i) {
            MotionEvent.PointerProperties props = new MotionEvent.PointerProperties();
            props.toolType = MotionEvent.TOOL_TYPE_FINGER;
            MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
            coords.orientation = 0;
            coords.size = 1;
            pointerProperties[i] = props;
            pointerCoords[i] = coords;
        }
        Notify.get().registerListener(this);
        is_stop.set(false);
        eventThread = new Thread(() -> {
            try {
                byte[] tEvent = new byte[128];
                while (!is_stop.get()) {
                    synchronized (eventLock) {
                        if (eventBuf.position() <= 0) eventLock.wait();
                        if (is_stop.get()) break;
                        eventBuf.flip();
                        eventBuf.get(tEvent, 0, 8);
                        int dLen = ByteUtil.bytes2Int(tEvent, 4, false);
                        eventBuf.get(tEvent, 8, dLen);
                        eventBuf.compact();
                    }
                    short type = ByteUtil.bytes2Short(tEvent, 2, true);

                    boolean isPowerKey = false;
                    if (Protocol.TYPE_INPUT_KEY_SINGLE == type || Protocol.TYPE_INPUT_KEY_MULTI == type) {
                        int keycode = ByteUtil.bytes2Short(tEvent, 18, false);
                        isPowerKey = keycode == KeyEvent.KEYCODE_POWER;
                    }

                    if (!isPowerKey && !powerManager.isScreenOn()) {
                        inputKey(KeyEvent.KEYCODE_POWER);
                        continue;
                    }

                    if (Protocol.TYPE_INPUT_TOUCH_SINGLE == type || Protocol.TYPE_INPUT_TOUCH_MULTI == type) {
                        int action = (tEvent[16] & 0xFF) << 8 | (tEvent[17] & 0xFF);
                        int pcount = tEvent[18] & 0xFF;
                        int orientation = tEvent[19] & 0xFF;
                        int cw = (tEvent[20] & 0xFF) << 8 | (tEvent[21] & 0xFF);
                        int ch = (tEvent[22] & 0xFF) << 8 | (tEvent[23] & 0xFF);
                        if (cw <= 0 || ch <= 0) {
                            FlyLog.e("error client width and height %dx%d", cw, ch);
                            continue;
                        }

                        long eventTime = ByteUtil.bytes2Long(tEvent, 24, false);
                        if (eventTime == 0) {
                            eventTime = SystemClock.uptimeMillis();
                            if (action == MotionEvent.ACTION_DOWN) {
                                lastDownTime = eventTime;
                            }
                        } else {
                            if (action == MotionEvent.ACTION_DOWN) {
                                verityTime = SystemClock.uptimeMillis() - eventTime;
                                lastDownTime = eventTime + verityTime;
                            }
                            eventTime = eventTime + verityTime;
                        }

                        for (int i = 0; i < pcount; i++) {
                            int px = ((tEvent[32 + i * 4] & 0xFF) << 8 | (tEvent[33 + i * 4] & 0xFF)) * screen_w / cw;
                            int py = ((tEvent[34 + i * 4] & 0xFF) << 8 | (tEvent[35 + i * 4] & 0xFF)) * screen_h / ch;
                            pointerProperties[i].id = i;
                            switch (orientation) {
                                case 1:
                                    pointerCoords[i].x = py;
                                    pointerCoords[i].y = screen_w - px;
                                    break;
                                case 2:
                                    pointerCoords[i].x = screen_w - px;
                                    pointerCoords[i].y = screen_h - py;
                                    break;
                                case 3:
                                    pointerCoords[i].x = screen_h - py;
                                    pointerCoords[i].y = px;
                                    break;
                                default:
                                    pointerCoords[i].x = px;
                                    pointerCoords[i].y = py;
                                    break;
                            }
                            if ((action & 0xFF) == MotionEvent.ACTION_UP || (action & 0xFF) == MotionEvent.ACTION_POINTER_UP) {
                                pointerCoords[i].pressure = 0.0f;
                            } else {
                                pointerCoords[i].pressure = 1.0f;
                            }
                        }
                        MotionEvent touch = MotionEvent.obtain(lastDownTime, eventTime, action, pcount, pointerProperties, pointerCoords, 0, MotionEvent.BUTTON_PRIMARY, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
                        serviceManager.getInputManager().injectInputEvent(touch, 0);
                    } else if (Protocol.TYPE_INPUT_KEY_SINGLE == type || Protocol.TYPE_INPUT_KEY_MULTI == type) {
                        inputKey(ByteUtil.bytes2Short(tEvent, 18, false));
                    } else if (Protocol.TYPE_INPUT_TEXT_SINGLE == type || Protocol.TYPE_INPUT_TEXT_MULTI == type) {
                        //TODO::text
                    }
                }
            } catch (Exception e) {
                FlyLog.e(e.toString());
            }
        }, "zebra_input");
        eventThread.start();
    }

    public void stop() {
        Notify.get().unregisterListener(this);
        is_stop.set(true);
        try {
            if (eventThread != null) {
                synchronized (eventLock) {
                    eventLock.notifyAll();
                }
                eventThread.join();
                eventThread = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        FlyLog.d("InputService exit!");
    }


    @Override
    public void notify(byte[] data, int size) {
        short type = ByteUtil.bytes2Short(data, 2, true);
        switch (type) {
            case Protocol.TYPE_INPUT_TOUCH_SINGLE:
            case Protocol.TYPE_INPUT_KEY_SINGLE:
            case Protocol.TYPE_INPUT_TEXT_SINGLE:
            case Protocol.TYPE_INPUT_TOUCH_MULTI:
            case Protocol.TYPE_INPUT_KEY_MULTI:
            case Protocol.TYPE_INPUT_TEXT_MULTI:
                synchronized (eventLock) {
                    if (eventBuf.position() + size > 1024) {
                        eventBuf.clear();
                    }
                    eventBuf.put(data, 0, size);
                    eventLock.notify();
                }
                break;
            case Protocol.TYPE_SCREEN_U_READY:
                if (!powerManager.isScreenOn()) {
                    synchronized (eventLock) {
                        eventBuf.put(INPUT_KEYCODE_POWER, 0, INPUT_KEYCODE_POWER.length);
                        eventLock.notify();
                    }
                }
                break;
        }
    }

    @Override
    public void handle(int type, byte[] data, int dsize, byte[] params, int psize) {

    }

    private boolean inputKey(int keycode) {
        try {
            long now = SystemClock.uptimeMillis();
            KeyEvent keydown = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keycode, 0);
            serviceManager.getInputManager().injectInputEvent(keydown, 0);
            KeyEvent keyup = new KeyEvent(now, now, KeyEvent.ACTION_UP, keycode, 0);
            return serviceManager.getInputManager().injectInputEvent(keyup, 0);
        } catch (Exception e) {
            FlyLog.e(e.toString());
        }
        return false;
    }
}
