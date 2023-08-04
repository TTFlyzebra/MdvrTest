package com.flyzebra.mdvr.store;

import static com.flyzebra.mdvr.Config.MAX_CAM;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;

import com.flyzebra.utils.FlyLog;

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

public class StorageService{
    private Context mContext;
    private StorageReceiver receiver = new StorageReceiver();
    private IntentFilter filter;
    private final Hashtable<Integer, StorageTasker> taskerMap = new Hashtable<>();

    public StorageService(Context context) {
        mContext = context;
    }

    public void onCreate() {
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        filter.addDataScheme("file");
        mContext.registerReceiver(receiver, filter);

        for (int i = 0; i < MAX_CAM; i++) {
            StorageTasker storageFilesave = new StorageTasker(i);
            storageFilesave.onCreate(getStorageTFcard());
            taskerMap.put(i, storageFilesave);
        }
    }

    public void onDestory() {
        mContext.unregisterReceiver(receiver);
        Enumeration<StorageTasker> elements = taskerMap.elements();
        while (elements.hasMoreElements()) {
            elements.nextElement().onDestory();
        }
    }

    private StorageTFcard getStorageTFcard() {
        StorageManager manager = (StorageManager) mContext.getSystemService(mContext.STORAGE_SERVICE);
        List<StorageVolume> list = manager.getStorageVolumes();
        for (StorageVolume volume : list) {
            if (volume == null || !volume.isRemovable()) continue;
            try {
                Class<?> myclass = Class.forName(volume.getClass().getName());
                Method getPath = myclass.getDeclaredMethod("getPath", null);
                getPath.setAccessible(true);
                String tfPath = (String) getPath.invoke(volume);
                if (!TextUtils.isEmpty(tfPath)) {
                    return new StorageTFcard(tfPath);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public class StorageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED) || intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                FlyLog.e("StorageReceiver Intent: %s", intent.toUri(0));
            }
        }
    }
}
