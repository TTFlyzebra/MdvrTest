package com.flyzebra.mdvr.store;

import static com.flyzebra.mdvr.Config.MAX_CAM;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;

import com.flyzebra.mdvr.Config;
import com.flyzebra.utils.FlyLog;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

public class StorageService {
    private final Context mContext;
    private final StorageReceiver receiver = new StorageReceiver();
    private final Hashtable<Integer, FileSaveTasker> taskerMap = new Hashtable<>();
    private boolean is_recored = false;
    private String rootPath = null;

    public StorageService(Context context) {
        mContext = context;
    }

    public void onCreate() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        filter.addDataScheme("file");
        mContext.registerReceiver(receiver, filter);
        startRecord();
    }

    public void onDestory() {
        mContext.unregisterReceiver(receiver);
        stopRecord();
    }


    public void startRecord() {
        if (is_recored) return;

        //没有TF卡不录像
        TFcard tfCard = getStorageTFcard();
        if (tfCard == null) {
            FlyLog.e("TF card not found!");
            return;
        }

        //空间不足4G不录像
        if (tfCard.freeBytes() < Config.MIN_STORE) {
            FlyLog.e("TF card free bytes is too small %d", tfCard.freeBytes());
            return;
        }

        //创建目录失败不录像
        rootPath = tfCard.getPath() + File.separator + "MD201";
        File file = new File(rootPath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                FlyLog.e("recored service mkdir %s failed!");
                return;
            }
        }

        for (int i = 0; i < MAX_CAM; i++) {
            FileSaveTasker tasker = new FileSaveTasker(i);
            tasker.onCreate(this);
            taskerMap.put(i, tasker);
        }
        is_recored = true;
    }

    public void stopRecord() {
        if (!is_recored) return;
        Enumeration<FileSaveTasker> elements = taskerMap.elements();
        while (elements.hasMoreElements()) {
            elements.nextElement().onDestory();
        }
        is_recored = false;
    }

    public TFcard getStorageTFcard() {
        StorageManager manager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        List<StorageVolume> list = manager.getStorageVolumes();
        for (StorageVolume volume : list) {
            if (volume == null || !volume.isRemovable()) continue;
            try {
                Class<?> myclass = Class.forName(volume.getClass().getName());
                Method getPath = myclass.getDeclaredMethod("getPath", null);
                getPath.setAccessible(true);
                String tfPath = (String) getPath.invoke(volume);
                if (!TextUtils.isEmpty(tfPath)) {
                    return new TFcard(tfPath);
                }
            } catch (Exception e) {
               FlyLog.e(e.toString());
            }
        }
        return null;
    }

    public String getSaveFileName(int channel) {
        if (TextUtils.isEmpty(rootPath)) return null;
        File rootFile = new File(rootPath);
        if (!rootFile.exists()) return null;
        //自动删除文件
        TFcard storageTFcard = getStorageTFcard();
        if (storageTFcard == null) return null;
        if (storageTFcard.freeBytes() < Config.MIN_STORE) {
            File[] files = rootFile.listFiles();
            if (files != null) {
                Arrays.sort(files, (f1, f2) -> {
                    long diff = f1.lastModified() - f2.lastModified();
                    if (diff > 0)
                        return 1;
                    else if (diff == 0)
                        return 0;
                    else
                        return -1;
                });
            } else {
                return null;
            }
            long lastModified = 0;
            for (File file : files) {
                if (file == null || !file.isFile()) continue;
                lastModified = file.lastModified();
                boolean flag = file.delete();
                if (flag) {
                    FlyLog.d("delete file %s", file.getAbsolutePath());
                    if (storageTFcard.freeBytes() > Config.MIN_STORE) break;
                } else {
                    FlyLog.e("delete file failed %s", file.getAbsolutePath());
                }
            }

            //删除同一时间的四个文件
            files = rootFile.listFiles();
            if (files != null) {
                Arrays.sort(files, (f1, f2) -> {
                    long diff = f1.lastModified() - f2.lastModified();
                    if (diff > 0)
                        return 1;
                    else if (diff == 0)
                        return 0;
                    else
                        return -1;
                });
                for (File file : files) {
                    if (file == null || !file.isFile()) continue;
                    if (file.lastModified() - lastModified < 30 * 1000L) {
                        if (file.delete()) {
                            FlyLog.d("delete file %s", file.getAbsolutePath());
                        } else {
                            FlyLog.e("delete file failed %s 2", file.getAbsolutePath());
                        }
                    }
                }
            }
        }
        if (storageTFcard.freeBytes() < Config.MIN_STORE) {
            FlyLog.e("TF card free bytes is too small %d", storageTFcard.freeBytes());
            return null;
        }
        String fileName = "CHANNEL_" + channel + "_" + System.currentTimeMillis() + ".mp4";
        return rootPath + File.separator + fileName;
    }

    public class StorageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED) || intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                if (!is_recored) {
                    startRecord();
                } else {
                    TFcard tFcard = getStorageTFcard();
                    if (tFcard == null) {
                        stopRecord();
                    }
                }
            }
        }
    }
}
