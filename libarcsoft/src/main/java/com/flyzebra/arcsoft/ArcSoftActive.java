package com.flyzebra.arcsoft;

import android.content.Context;

import com.arcsoft.visdrive.sdk.ArcErrorInfo;
import com.arcsoft.visdrive.sdk.ArcVisDriveEngine;
import com.arcsoft.visdrive.sdk.constant.common.ArcModType;
import com.arcsoft.visdrive.sdk.model.common.ArcActivateStatus;
import com.arcsoft.visdrive.sdk.model.common.ArcActiveEnvParam;
import com.flyzebra.utils.IDUtil;

import java.util.concurrent.atomic.AtomicBoolean;

public class ArcSoftActive {
    private Context mContext;
    private AtomicBoolean is_active = new AtomicBoolean(false);
    private ArcVisDriveEngine arcVisDriveEngine;

    private ArcSoftActive() {
    }

    private static class ActiveArcSoftHolder {
        public static final ArcSoftActive sInstance = new ArcSoftActive();
    }

    public static ArcSoftActive get() {
        return ArcSoftActive.ActiveArcSoftHolder.sInstance;
    }

    public void init(Context context) {
        mContext = context;
        is_active.set(false);
        arcVisDriveEngine = new ArcVisDriveEngine();
    }

    public boolean active(Context context, String key1, String key2) {
        ArcModType[] actives = new ArcModType[3];
        actives[0] = ArcModType.TYPE_ADAS;
        actives[1] = ArcModType.TYPE_DMS;
        actives[2] = ArcModType.TYPE_FR;

        ArcActiveEnvParam arcActiveEnvParam = new ArcActiveEnvParam();
        arcActiveEnvParam.storagePath = context.getExternalFilesDir("arcsoft").getAbsolutePath();
        arcActiveEnvParam.IMEI = IDUtil.getIMEI(context);

        int ret = ArcVisDriveEngine.activate(key1, key2, actives, arcActiveEnvParam);
        if (ret == ArcErrorInfo.ARC_ERROR_OK) {
            //ArcActiveFileInfo activeFile = new ArcActiveFileInfo();
            //String activateFilePath = context.getExternalFilesDir("arcsoft").getAbsolutePath() + File.separator + "ArcDriveActiveFile.dat";
            //int result = ArcVisDriveEngine.getActivateFileInfo(activateFilePath, activeFile);
            //if (result == ArcErrorInfo.ARC_ERROR_OK) {
            //    FlyLog.v(activeFile.toString());
            //}
            is_active.set(true);
            return true;
        }
        return false;
    }

    public boolean isActive() {
        return is_active.get();
    }

    public boolean isAdasActive() {
        ArcActivateStatus activateStatus = new ArcActivateStatus();
        int result = ArcVisDriveEngine.getActivateStatus(ArcModType.TYPE_ADAS, activateStatus);
        return result == ArcErrorInfo.ARC_ERROR_OK && activateStatus.status == 0;
    }

    public boolean isDmsActive() {
        ArcActivateStatus activateStatus = new ArcActivateStatus();
        int result = ArcVisDriveEngine.getActivateStatus(ArcModType.TYPE_DMS, activateStatus);
        return result == ArcErrorInfo.ARC_ERROR_OK && activateStatus.status == 0;
    }

    public boolean isFrActive() {
        ArcActivateStatus activateStatus = new ArcActivateStatus();
        int result = ArcVisDriveEngine.getActivateStatus(ArcModType.TYPE_FR, activateStatus);
        return result == ArcErrorInfo.ARC_ERROR_OK && activateStatus.status == 0;
    }
}
