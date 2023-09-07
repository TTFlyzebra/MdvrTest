/**
 * FileName: Notify
 * Author: FlyZebra
 * Email:flycnzebra@gmail.com
 * Date: 2023/6/23 10:31
 * Description:
 */
package com.flyzebra.core.notify;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;

import com.flyzebra.utils.ByteUtil;
import com.flyzebra.utils.FlyLog;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Notify {
    private static final List<INotify> notifys = new CopyOnWriteArrayList<>();
    private final Object listLock = new Object();
    private final AtomicInteger listCount = new AtomicInteger(0);
    private static final HandlerThread mDataThread = new HandlerThread("Notify_data");

    static {
        mDataThread.start();
    }

    private static final Handler tHandler = new Handler(mDataThread.getLooper());

    private Notify() {
        listCount.set(0);
    }

    private static class NotifyHolder {
        public static final Notify sInstance = new Notify();
    }

    public static Notify get() {
        return NotifyHolder.sInstance;
    }

    public void registerListener(INotify notify) {
        while (listCount.get() > 0) {
            FlyLog.d("registerListener did not end %d...", listCount.get());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        synchronized (listLock) {
            notifys.add(notify);
        }
    }

    public void unregisterListener(INotify notify) {
        while (listCount.get() > 0) {
            FlyLog.d("unregisterListener did not end %d...", listCount.get());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        synchronized (listLock) {
            notifys.remove(notify);
        }
    }

    public void notifydata(byte[] data, int size) {
        long stime = SystemClock.uptimeMillis();
        listCount.incrementAndGet();
        for (INotify notify : notifys) {
            notify.notify(data, size);
        }
        listCount.decrementAndGet();
        synchronized (listLock) {
            listLock.notifyAll();
        }
        long utime = SystemClock.uptimeMillis() - stime;
        if (utime > 50) {
            FlyLog.w("Notify notifydata use time %d, size %d", utime, size);
        }
    }

    public void handledata(int type, byte[] data, int size, byte[] params) {
        long stime = SystemClock.uptimeMillis();
        listCount.incrementAndGet();
        for (INotify notify : notifys) {
            notify.handle(type, data, size, params);
        }
        listCount.decrementAndGet();
        synchronized (listLock) {
            listLock.notifyAll();
        }
        long utime = SystemClock.uptimeMillis() - stime;
        if (utime > 50) {
            FlyLog.w("Notify handledata use type %d, time %d, size %d", type, utime, size);
        }
    }

    public void miniNotify(byte[] command, int size, long tid, long uid, byte[] params) {
        byte[] sendcmd = new byte[size];
        System.arraycopy(command, 0, sendcmd, 0, size);
        int start = 8;
        if (tid != 0) {
            ByteUtil.longToBytes(tid, sendcmd, start, true);
            start += 8;
        }
        if (uid != 0) {
            ByteUtil.longToBytes(uid, sendcmd, start, true);
            start += 8;
        }
        if (params != null) {
            System.arraycopy(sendcmd, start, params, 0, size - start);
        }
        tHandler.post(() -> notifydata(sendcmd, size));
    }
}
