package com.flyzebra.mdvr.store;

import android.os.StatFs;
import android.text.TextUtils;

public class TFcard {
    private String mPath;
    public TFcard(String path) {
        mPath = path;
    }

    public String getPath() {
        return mPath;
    }

    public long freeBytes() {
        if (TextUtils.isEmpty(mPath)) return 0;
        StatFs statFs = new StatFs(mPath);
        return statFs.getFreeBytes();
    }

    public long totalBytes() {
        if (TextUtils.isEmpty(mPath)) return 0;
        StatFs statFs = new StatFs(mPath);
        return statFs.getTotalBytes();
    }
}
