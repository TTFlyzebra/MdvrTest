package com.flyzebra.arcsoft;

import android.content.Context;

import com.arcsoft.visdrive.sdk.ArcErrorInfo;
import com.arcsoft.visdrive.sdk.ArcVisDriveEngine;
import com.arcsoft.visdrive.sdk.constant.common.ArcAlarmSensitivityLevel;
import com.arcsoft.visdrive.sdk.constant.common.ArcImageFormat;
import com.arcsoft.visdrive.sdk.constant.common.ArcModType;
import com.arcsoft.visdrive.sdk.constant.dms.ArcDMSAlarmType;
import com.arcsoft.visdrive.sdk.constant.dms.ArcDMSDetectMaskType;
import com.arcsoft.visdrive.sdk.model.common.ArcDrivingStatus;
import com.arcsoft.visdrive.sdk.model.common.ArcInitParamInfo;
import com.arcsoft.visdrive.sdk.model.common.ArcInitParamInfoDetail;
import com.arcsoft.visdrive.sdk.model.dms.ArcDMSAlarmParam;
import com.arcsoft.visdrive.sdk.model.dms.ArcDMSDetectResult;
import com.arcsoft.visdrive.sdk.model.dms.ArcDMSDistractScope;
import com.arcsoft.visdrive.sdk.model.dms.ArcDMSInitParam;
import com.flyzebra.utils.FlyLog;

import java.nio.ByteBuffer;

public class ArcSoftDms {
    private Context mContext;
    private ArcVisDriveEngine engine;

    public ArcSoftDms(Context context) {
        mContext = context;
        engine = new ArcVisDriveEngine();
    }

    public boolean initDms() {
        ArcDMSInitParam dmsInitParam = new ArcDMSInitParam();
        dmsInitParam.detectMask = ArcDMSDetectMaskType.MOD_DMS_CALL |
                ArcDMSDetectMaskType.MOD_DMS_SMOKE |
                ArcDMSDetectMaskType.MOD_DMS_CLOSE_EYE |
                ArcDMSDetectMaskType.MOD_DMS_YAWN |
                ArcDMSDetectMaskType.MOD_DMS_DISTRACT |
                ArcDMSDetectMaskType.MOD_DMS_DRIVER_ABNORMAL |
                ArcDMSDetectMaskType.MOD_DMS_IR_BLOCKING |
                ArcDMSDetectMaskType.MOD_DMS_LENS_COVERED |
                ArcDMSDetectMaskType.MOD_DMS_SEATBELT_UNFASTENED;

        ArcInitParamInfoDetail initParamDetail = new ArcInitParamInfoDetail();
        initParamDetail.modType = ArcModType.TYPE_DMS;
        initParamDetail.arcInitParamBase = dmsInitParam;
        ArcInitParamInfo arcInfoParam = new ArcInitParamInfo();
        arcInfoParam.arcInitParamInfoDetailArray = new ArcInitParamInfoDetail[]{initParamDetail};
        int ret = engine.init(arcInfoParam);
        if (ret == ArcErrorInfo.ARC_ERROR_OK) {
            FlyLog.i("DMS初始化成功");
            return true;
        } else {
            FlyLog.i("DMS初始化失败：%d", ret);
            return false;
        }
    }

    public void initDmsParam() {
        /**
         * 这里只模拟设置DMS_CALL报警参数，其他报警参数设置类似
         */
        ArcDMSAlarmParam dmsAlarmParam = new ArcDMSAlarmParam();
        int result = engine.getDMSAlarmParam(ArcDMSAlarmType.ALARM_DMS_CALL, dmsAlarmParam);
        //FlyLog.i("getDMSAlarmParam1:%s", dmsAlarmParam.toString());
        if (result == ArcErrorInfo.ARC_ERROR_OK) {
            dmsAlarmParam.sensitivityLevel = ArcAlarmSensitivityLevel.ALARM_SENSITIVITY_MEDIUM;
            /**
             * 以下设置参数仅供测试使用，实际使用请根据真实环境设置
             */
            dmsAlarmParam.arcAlarmParam.interval = 6;
            dmsAlarmParam.arcAlarmParam.speedThreshold = 30;
            engine.setDMSAlarmParam(ArcDMSAlarmType.ALARM_DMS_CALL, dmsAlarmParam);
            engine.setDMSAlarmParam(ArcDMSAlarmType.ALARM_DMS_SMOKE, dmsAlarmParam);
            engine.setDMSAlarmParam(ArcDMSAlarmType.ALARM_DMS_CLOSE_EYE, dmsAlarmParam);
            engine.setDMSAlarmParam(ArcDMSAlarmType.ALARM_DMS_YAWN, dmsAlarmParam);
            engine.setDMSAlarmParam(ArcDMSAlarmType.ALARM_DMS_DISTRACT, dmsAlarmParam);
            engine.setDMSAlarmParam(ArcDMSAlarmType.ALARM_DMS_DRIVER_ABNORMAL, dmsAlarmParam);
            engine.setDMSAlarmParam(ArcDMSAlarmType.ALARM_DMS_IR_BLOCKING, dmsAlarmParam);
            engine.setDMSAlarmParam(ArcDMSAlarmType.ALARM_DMS_LENS_COVERED, dmsAlarmParam);
            engine.setDMSAlarmParam(ArcDMSAlarmType.ALARM_DMS_SEATBELT_UNFASTENED, dmsAlarmParam);
        }
        engine.getDMSAlarmParam(ArcDMSAlarmType.ALARM_DMS_CALL, dmsAlarmParam);
        //FlyLog.i("getDMSAlarmParam2:%s", dmsAlarmParam.toString());
        ArcDMSDistractScope dmsScope = new ArcDMSDistractScope();
        result = engine.getDMSDistractScopeParam(dmsScope);
        //FlyLog.i("getDMSDistractScopeParam result:" + result);
        if (result == ArcErrorInfo.ARC_ERROR_OK) {
            //FlyLog.i("getDMSDistractScopeParam:" + dmsScope);
            /**
             * 以下为模拟修改数值，实际使用请根据真实环境设置
             */
            dmsScope.leftYaw = -28;
            dmsScope.rightYaw = 35;
            dmsScope.upPitch = 35;
            dmsScope.downPitch = -20;
            result = engine.setDMSDistractScopeParam(dmsScope);
            //FlyLog.i("setDMSDistractScopeParam result:" + result);
            if (result == ArcErrorInfo.ARC_ERROR_OK) {
                engine.getDMSDistractScopeParam(dmsScope);
                //FlyLog.i("getDMSDistractScopeParam2:" + dmsScope);
            }
        }
        /**
         * 设置驾驶状态
         */
        ArcDrivingStatus driveStatus = new ArcDrivingStatus();
        driveStatus.speed = 33;
        result = engine.setDrivingStatus(driveStatus);
        //FlyLog.i("setDrivingStatus result:" + result);
    }

    public ArcDMSDetectResult detectNV12(ByteBuffer buffer, int size, int width, int height) {
        ArcDMSDetectResult dmsResult = new ArcDMSDetectResult();
        int ret = engine.detectDMS(
                width,
                height,
                ArcImageFormat.ARC_IMAGE_FORMAT_NV12,
                buffer,
                dmsResult);
        if (ret == ArcErrorInfo.ARC_ERROR_OK) {
            if (dmsResult.alarmMask > 0) {
                FlyLog.i(getDMSAlarmText(dmsResult.alarmMask));
            }
            return dmsResult;
        } else {
            FlyLog.e("ArcADASDetectResult result=%d", ret);
            return null;
        }
    }

    //int MOD_DMS_CALL 1 打电话
    //int MOD_DMS_SMOKE 2 抽烟
    //int MOD_DMS_CLOSE_EYE 4 闭眼
    //int MOD_DMS_YAWN 8 打哈欠
    //int MOD_DMS_DISTRACT 16 分心
    //int MOD_DMS_DRIVER_ABNORMAL 32 驾驶员异常
    //int MOD_DMS_IR_BLOCKING 64 红外阻断
    //int MOD_DMS_LENS_COVERED 128 摄像头遮挡
    //int MOD_DMS_SEATBELT_UNFASTENED 256 未系安全带
    private String getDMSAlarmText(int alarmMask) {
        switch (alarmMask) {
            case ArcDMSDetectMaskType.MOD_DMS_CALL:
                return "打电话报警";
            case ArcDMSDetectMaskType.MOD_DMS_SMOKE:
                return "抽烟报警";
            case ArcDMSDetectMaskType.MOD_DMS_CLOSE_EYE:
                return "闭眼报警";
            case ArcDMSDetectMaskType.MOD_DMS_YAWN:
                return "打哈欠报警";
            case ArcDMSDetectMaskType.MOD_DMS_DISTRACT:
                return "分心报警";
            case ArcDMSDetectMaskType.MOD_DMS_DRIVER_ABNORMAL:
                return "驾驶员异常报警";
            case ArcDMSDetectMaskType.MOD_DMS_IR_BLOCKING:
                return "红外阻断";
            case ArcDMSDetectMaskType.MOD_DMS_LENS_COVERED:
                return "镜头遮挡";
            case ArcDMSDetectMaskType.MOD_DMS_SEATBELT_UNFASTENED:
                return "未系安全带";
            default:
                return "无";
        }
    }

    public void unInitAdas() {
        engine.unInit();
    }
}
