/**
 * FileName: Notify
 * Author: FlyZebra
 * Email:flycnzebra@gmail.com
 * Date: 2023/6/23 10:31
 * Description:
 */
package com.flyzebra.notify;

import android.os.Handler;
import android.os.HandlerThread;

import com.flyzebra.utils.ByteUtil;

import java.util.ArrayList;
import java.util.List;

public class Notify {
    private static final List<INotify> notifys = new ArrayList<>();
    private final Object listLock = new Object();
    private static final HandlerThread mDataThread = new HandlerThread("Notify_data");

    static {
        mDataThread.start();
    }

    private static final Handler tHandler = new Handler(mDataThread.getLooper());

    private Notify() {
    }

    private static class NotifyHolder {
        public static final Notify sInstance = new Notify();
    }

    public static Notify get() {
        return NotifyHolder.sInstance;
    }

    public void registerListener(INotify notify) {
        synchronized (listLock){
            notifys.add(notify);
        }
    }

    public void unregisterListener(INotify notify) {
        synchronized (listLock){
            notifys.remove(notify);
        }
    }

    public void notifydata(byte[] data, int size) {
        synchronized (listLock) {
            for (INotify notify : notifys) {
                notify.notify(data, size);
            }
        }
    }

    public void handledata(int type, byte[] data, int size, byte[] params) {
        synchronized (listLock) {
            for (INotify notify : notifys) {
                notify.handle(type, data, size, params);
            }
        }
    }

    public void miniNotify(byte[] command, int size, long tid, long uid, byte[] params) {
        byte[] sendcmd = new byte[size];
        System.arraycopy(sendcmd, 0, command, 0, size);
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
