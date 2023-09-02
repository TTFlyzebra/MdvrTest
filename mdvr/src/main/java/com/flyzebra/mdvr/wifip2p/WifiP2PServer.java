package com.flyzebra.mdvr.wifip2p;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.flyzebra.utils.FlyLog;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class WifiP2PServer {
    private Context mContext;
    private AtomicBoolean is_stop = new AtomicBoolean(true);
    private Thread workThread;
    private List<WifiP2PClient> wifiNetClients = new ArrayList<>();
    private static final Handler mHandler = new Handler(Looper.getMainLooper());
    private ServerSocket serverSocket = null;

    public WifiP2PServer(Context context) {
        mContext = context;
    }

    public void start() {
        FlyLog.d("WifiP2PSocket start!");
        is_stop.set(false);
        workThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(9088);
            } catch (IOException e) {
                FlyLog.e(e.toString());
                return;
            }
            while (!is_stop.get()) {
                try {
                    Socket client = serverSocket.accept();
                    FlyLog.e("wifip2p client accept!");
                    final WifiP2PClient wifiNetClient = new WifiP2PClient(mContext, this, client);
                    mHandler.post(() -> wifiNetClients.add(wifiNetClient));
                } catch (IOException e) {
                    FlyLog.d(e.toString());
                }
            }

            mHandler.post(() -> {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    serverSocket = null;
                }
            });
        }, "wifip2p_server");
        workThread.start();
    }

    public void stop() {
        if (is_stop.get()) return;
        mHandler.removeCallbacksAndMessages(null);
        is_stop.set(true);
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            serverSocket = null;
        }
        if (workThread != null) {
            try {
                workThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            workThread = null;
        }
        for (WifiP2PClient client : wifiNetClients) {
            client.stop();
        }
        wifiNetClients.clear();
        FlyLog.d("WifiP2PSocket stop!");
    }

    public void disconnected(final WifiP2PClient wifiNetClient) {
        mHandler.post(() -> wifiNetClients.remove(wifiNetClient));
    }
}
