package com.flyzebra.arcsoft;

import android.content.Context;

import com.arcsoft.visdrive.sdk.ArcErrorInfo;
import com.arcsoft.visdrive.sdk.ArcVisDriveEngine;
import com.arcsoft.visdrive.sdk.constant.adas.ArcADASAlarmType;
import com.arcsoft.visdrive.sdk.constant.adas.ArcADASDetectMaskType;
import com.arcsoft.visdrive.sdk.constant.common.ArcImageFormat;
import com.arcsoft.visdrive.sdk.constant.common.ArcModType;
import com.arcsoft.visdrive.sdk.model.adas.ArcADASAlarmParam;
import com.arcsoft.visdrive.sdk.model.adas.ArcADASCalibInfo;
import com.arcsoft.visdrive.sdk.model.adas.ArcADASCalibResult;
import com.arcsoft.visdrive.sdk.model.adas.ArcADASDetectResult;
import com.arcsoft.visdrive.sdk.model.adas.ArcADASImageInitInfo;
import com.arcsoft.visdrive.sdk.model.adas.ArcADASInitParam;
import com.arcsoft.visdrive.sdk.model.adas.ArcADASIntrinsicParam;
import com.arcsoft.visdrive.sdk.model.common.ArcDrivingStatus;
import com.arcsoft.visdrive.sdk.model.common.ArcInitParamInfo;
import com.arcsoft.visdrive.sdk.model.common.ArcInitParamInfoDetail;
import com.flyzebra.utils.FlyLog;

import java.nio.ByteBuffer;

public class ArcSoftAdas {
    private Context mContext;
    private ArcVisDriveEngine engine;

    public ArcSoftAdas(Context context) {
        mContext = context;
        engine = new ArcVisDriveEngine();
    }

    public ArcADASCalibResult setAdasCalibInfo(ArcADASCalibInfo calibInfo) {
        ArcADASCalibResult calibResult = new ArcADASCalibResult();
        int ret = engine.setADASCalibInfo(calibInfo, calibResult);
        if (ret == ArcErrorInfo.ARC_ERROR_OK) {
            return calibResult;
        } else {
            return null;
        }
    }

    public boolean initAdas(ArcADASCalibInfo calibInfo) {
        ArcADASInitParam initParam = new ArcADASInitParam();
        initParam.detectMask = ArcADASDetectMaskType.MOD_ADAS_LDW |
                ArcADASDetectMaskType.MOD_ADAS_FCW |
                ArcADASDetectMaskType.MOD_ADAS_HMW |
                ArcADASDetectMaskType.MOD_ADAS_PCW;
        ArcADASImageInitInfo adasImageInfo = new ArcADASImageInitInfo();
        adasImageInfo.width = 1280;
        adasImageInfo.height = 720;
        adasImageInfo.imageFormat = ArcImageFormat.ARC_IMAGE_FORMAT_NV12.getValue();
        initParam.arcADASImageInitInfo = adasImageInfo;

        ArcADASIntrinsicParam intrinsicParam = new ArcADASIntrinsicParam();
        intrinsicParam.fx = 1186.097949;
        intrinsicParam.fy = 1151.994581;
        intrinsicParam.cx = 614.472595;
        intrinsicParam.cy = 312.694375;
        intrinsicParam.skew = 0.0;
        intrinsicParam.k1 = -0.461251;
        intrinsicParam.k2 = 0.25828;
        intrinsicParam.k3 = -0.026458;
        intrinsicParam.p1 = 0.0;
        intrinsicParam.p2 = 0.0;
        intrinsicParam.width = 1280;
        intrinsicParam.height = 720;
        intrinsicParam.checksum = 526503.007182;
        intrinsicParam.fisheye = false;
        initParam.arcADASIntrinsicParam = intrinsicParam;

        initParam.arcADASCalibInfo = calibInfo;

        ArcADASCalibResult calibResult = new ArcADASCalibResult();
        calibResult.r1 = -1.179168;
        calibResult.r2 = 1.1453419;
        calibResult.r3 = -1.214212;
        calibResult.t1 = 0.0;
        calibResult.t2 = 0.0;
        calibResult.t3 = 1350.0;
        calibResult.pitch = 3.3437219;
        calibResult.yaw = -1.6674138;
        calibResult.roll = 0.0;
        initParam.arcADASCalibResult = calibResult;

        ArcInitParamInfoDetail initParamDetail = new ArcInitParamInfoDetail();
        initParamDetail.modType = ArcModType.TYPE_ADAS;
        initParamDetail.arcInitParamBase = initParam;
        ArcInitParamInfo arcInfoParam = new ArcInitParamInfo();
        arcInfoParam.arcInitParamInfoDetailArray = new ArcInitParamInfoDetail[]{initParamDetail};
        int result = engine.init(arcInfoParam);
        if (result == ArcErrorInfo.ARC_ERROR_OK) {
            FlyLog.i("ADAS 初始化成功");
            return true;
        } else {
            FlyLog.e("ADAS 初始化失败：code[%d]", result);
            return false;
        }
    }

    public void initAdasParam() {
        ArcADASAlarmParam alarmParam = new ArcADASAlarmParam();
        int result = engine.getADASAlarmParam(ArcADASAlarmType.ALARM_ADAS_FCW, alarmParam);
        //FlyLog.i("getADASAlarmParam:" + alarmParam);
        if (result == 0) {
            alarmParam.sensitivityLevel = 1;
            alarmParam.arcAlarmParam.interval = 4;
            alarmParam.arcAlarmParam.speedThreshold = 30;
            engine.setADASAlarmParam(ArcADASAlarmType.ALARM_ADAS_LDW, alarmParam);
            engine.setADASAlarmParam(ArcADASAlarmType.ALARM_ADAS_FCW, alarmParam);
            engine.setADASAlarmParam(ArcADASAlarmType.ALARM_ADAS_HMW, alarmParam);
            result = engine.setADASAlarmParam(ArcADASAlarmType.ALARM_ADAS_PCW, alarmParam);
            //FlyLog.i("setADASAlarmParam result:" + result);
            ArcDrivingStatus driveStatus = new ArcDrivingStatus();
            driveStatus.speed = 33;
            result = engine.setDrivingStatus(driveStatus);
            //FlyLog.i("setDrivingStatus result:" + result);
        }
    }

    public ArcADASDetectResult detectNV12(ByteBuffer buffer, int size, int width, int height) {
        ArcADASDetectResult adasResult = new ArcADASDetectResult();
        int ret = engine.detectADAS(
                width,
                height,
                ArcImageFormat.ARC_IMAGE_FORMAT_NV12,
                buffer,
                adasResult);
        if (ret == ArcErrorInfo.ARC_ERROR_OK) {
            if (adasResult.alarmMask > 0) {
                FlyLog.i(getADASAlarmText(adasResult.alarmMask));
            }
            return adasResult;
        } else {
            FlyLog.e("ArcADASDetectResult result=%d", ret);
            return null;
        }
    }

    //int MOD_ADAS_LDW 1 车道偏离
    //int MOD_ADAS_FCW 2 前向碰撞
    //int MOD_ADAS_HMW 4 车距过近
    //int MOD_ADAS_PCW 8 行人碰撞
    private String getADASAlarmText(int alarmMask) {
        switch (alarmMask) {
            case ArcADASDetectMaskType.MOD_ADAS_LDW:
                return "车道偏离";
            case ArcADASDetectMaskType.MOD_ADAS_FCW:
                return "前向碰撞";
            case ArcADASDetectMaskType.MOD_ADAS_HMW:
                return "车距过近";
            case ArcADASDetectMaskType.MOD_ADAS_PCW:
                return "行人碰撞";
            default:
                return "无";
        }
    }

    public void unInitAdas() {
        engine.unInit();
    }
}
