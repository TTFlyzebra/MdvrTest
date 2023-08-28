package com.flyzebra.mdvr.arcsoft;

import android.content.Context;

import com.arcsoft.visdrive.sdk.ArcErrorInfo;
import com.arcsoft.visdrive.sdk.ArcVisDriveEngine;
import com.arcsoft.visdrive.sdk.constant.common.ArcModType;
import com.arcsoft.visdrive.sdk.model.common.ArcActivateStatus;
import com.arcsoft.visdrive.sdk.model.common.ArcActiveEnvParam;
import com.arcsoft.visdrive.sdk.model.common.ArcActiveFileInfo;
import com.flyzebra.utils.FlyLog;
import com.flyzebra.utils.IDUtil;

import java.io.File;

public class ArcsoftActive {
    private Context mContext;

    public ArcsoftActive(Context context) {
        mContext = context;
    }

    public void activeOnline() {
        ArcModType[] actives = new ArcModType[3];
        actives[0] = ArcModType.TYPE_ADAS;
        actives[1] = ArcModType.TYPE_DMS;
        actives[2] = ArcModType.TYPE_FR;

        ArcActiveEnvParam arcActiveEnvParam = new ArcActiveEnvParam();
        arcActiveEnvParam.storagePath = mContext.getExternalFilesDir("activate").getAbsolutePath();
        arcActiveEnvParam.IMEI = IDUtil.getIMEI(mContext);

        int activateResult = ArcVisDriveEngine.activate(
                "vdpeYwDGohrtjAyMZdQMwNS3DPQF66",
                "dyqtqEpNyKLUZaMVAZdFhgtjskymaR",
                actives,
                arcActiveEnvParam);

        if (activateResult == ArcErrorInfo.ARC_ERROR_OK) {
            ArcActiveFileInfo activeFile = new ArcActiveFileInfo();
            String activateFilePath = mContext.getExternalFilesDir("activate").getAbsolutePath() + File.separator + "ArcDriveActiveFile.dat";
            int getActivateFileInfoResult = ArcVisDriveEngine.getActivateFileInfo(activateFilePath, activeFile);
            if (getActivateFileInfoResult == ArcErrorInfo.ARC_ERROR_OK) {
                FlyLog.i(activeFile.fileInfo);
            }
        }

        ArcActivateStatus activateStatus = new ArcActivateStatus();
        int result = ArcVisDriveEngine.getActivateStatus(ArcModType.TYPE_DMS, activateStatus);
        if (result == ArcErrorInfo.ARC_ERROR_OK) {
            FlyLog.i("ArcActivateStatus:" + activateStatus.status);
        }
    }
}
