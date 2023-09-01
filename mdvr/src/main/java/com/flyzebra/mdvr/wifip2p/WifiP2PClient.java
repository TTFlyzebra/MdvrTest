package com.flyzebra.mdvr.wifip2p;

import android.content.Context;

import com.flyzebra.utils.FlyLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class WifiP2PClient {
    private Context mContext;
    private WifiP2PSocket mServer;
    private Socket mSocket;
    private final AtomicBoolean is_stop = new AtomicBoolean(true);
    private Thread recvThread;
    private Thread sendThread;
    private final Object sendLock = new Object();
    private ByteBuffer sendBuf = ByteBuffer.allocateDirect(2 * 1024 * 1024);

    public WifiP2PClient(Context context, WifiP2PSocket server, Socket socket) {
        mContext = context;
        mServer = server;
        mSocket = socket;
    }

    public void start() {
        is_stop.set(false);
        recvThread = new Thread(() -> {
            byte[] recvBuf = new byte[2 * 1024 * 1024];
            InputStream inps = null;
            try {
                inps = mSocket.getInputStream();
            } catch (IOException e) {
                FlyLog.e(e.toString());
                mServer.disconnected(this);
                return;
            }
            while (!is_stop.get()) {
                try {
                    int readLen = inps.read(recvBuf);
                    FlyLog.e("recv %s", recvBuf);
                } catch (IOException e) {
                    FlyLog.e(e.toString());
                    mServer.disconnected(this);
                    return;
                }
            }
        }, "wifip2p_recv");
        recvThread.start();
        sendThread = new Thread(() -> {
            OutputStream outs = null;
            try {
                outs = mSocket.getOutputStream();
            } catch (IOException e) {
                FlyLog.e(e.toString());
                mServer.disconnected(this);
                return;
            }
            while (!is_stop.get()) {
                synchronized (sendLock) {
                    if (!is_stop.get() && sendBuf.position() <= 0) {
                        try {
                            sendLock.wait();
                        } catch (InterruptedException e) {
                            FlyLog.e(e.toString());
                            mServer.disconnected(this);
                            return;
                        }
                    }
                    if (is_stop.get()) return;
                    try {
                        outs.write(sendBuf.array());
                    } catch (IOException e) {
                        FlyLog.e(e.toString());
                        mServer.disconnected(this);
                        return;
                    }
                }
            }
        }, "wifip2p_send");
        sendThread.start();
    }

    public void stop() {
        is_stop.set(true);
        try {
            mSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (recvThread != null) {
            try {
                recvThread.join();
                recvThread = null;
            } catch (InterruptedException e) {
                FlyLog.e(e.toString());
            }
        }
        if (sendThread != null) {
            try {
                sendThread.join();
                sendThread = null;
            } catch (InterruptedException e) {
                FlyLog.e(e.toString());
            }
        }
    }
}
