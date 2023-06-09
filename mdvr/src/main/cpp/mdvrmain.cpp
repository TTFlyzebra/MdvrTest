#include <jni.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <android/log.h>

#define TAG "MDVR-JNI"
#define FLOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__);
#define FLOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__);
#define FLOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__);

#define BASE_VIDIOC_PRIVATE	192		/* 192-255 are private */
#define MAX_SENSOR_NAME 32

enum camb_position_t {
    BACK_CAMERA_B,
    FRONT_CAMERA_B,
    AUX_CAMERA_B = 0x100,
    INVALID_CAMERA_B,
};

enum msm_sensor_cfg_type_t {
    CFG_SET_SLAVE_INFO,
    CFG_SLAVE_READ_I2C,
    CFG_WRITE_I2C_ARRAY,
    CFG_SLAVE_WRITE_I2C_ARRAY,
    CFG_WRITE_I2C_SEQ_ARRAY,
    CFG_POWER_UP,
    CFG_POWER_DOWN,
    CFG_SET_STOP_STREAM_SETTING,
    CFG_GET_SENSOR_INFO,
    CFG_GET_SENSOR_INIT_PARAMS,
    CFG_SET_INIT_SETTING,
    CFG_SET_RESOLUTION,
    CFG_SET_STOP_STREAM,
    CFG_SET_START_STREAM,
    CFG_SET_SATURATION,
    CFG_SET_CONTRAST,
    CFG_SET_SHARPNESS,
    CFG_SET_ISO,
    CFG_SET_EXPOSURE_COMPENSATION,
    CFG_SET_ANTIBANDING,
    CFG_SET_BESTSHOT_MODE,
    CFG_SET_EFFECT,
    CFG_SET_WHITE_BALANCE,
    CFG_SET_AUTOFOCUS,
    CFG_CANCEL_AUTOFOCUS,
    CFG_SET_STREAM_TYPE,
    CFG_SET_I2C_SYNC_PARAM,
    CFG_WRITE_I2C_ARRAY_ASYNC,
    CFG_WRITE_I2C_ARRAY_SYNC,
    CFG_WRITE_I2C_ARRAY_SYNC_BLOCK,
};

enum sensor_sub_module_t {
    SUB_MODULE_SENSOR,
    SUB_MODULE_CHROMATIX,
    SUB_MODULE_ACTUATOR,
    SUB_MODULE_EEPROM,
    SUB_MODULE_LED_FLASH,
    SUB_MODULE_STROBE_FLASH,
    SUB_MODULE_CSID,
    SUB_MODULE_CSID_3D,
    SUB_MODULE_CSIPHY,
    SUB_MODULE_CSIPHY_3D,
    SUB_MODULE_OIS,
    SUB_MODULE_EXT,
    SUB_MODULE_IR_LED,
    SUB_MODULE_IR_CUT,
    SUB_MODULE_LASER_LED,
    SUB_MODULE_MAX,
};

struct msm_sensor_info_t {
    char     sensor_name[MAX_SENSOR_NAME];
    uint32_t session_id;
    int32_t  subdev_id[SUB_MODULE_MAX];
    int32_t  subdev_intf[SUB_MODULE_MAX];
    uint8_t  is_mount_angle_valid;
    uint32_t sensor_mount_angle;
    int modes_supported;
    enum camb_position_t position;
};

struct sensorb_cfg_data {
    int cfgtype;
    struct msm_sensor_info_t  sensor_info;
};

enum csid_cfg_type_t {
    CSID_INIT,
    CSID_CFG,
    CSID_TESTMODE_CFG,
    CSID_RELEASE,
};

struct csid_cfg_data {
    enum csid_cfg_type_t cfgtype;
    uint32_t csid_version;
};

#define VIDIOC_MSM_SENSOR_CFG \
	_IOWR('V', BASE_VIDIOC_PRIVATE + 1, struct sensorb_cfg_data)

#define VIDIOC_MSM_CSID_IO_CFG \
	_IOWR('V', BASE_VIDIOC_PRIVATE + 5, struct csid_cfg_data)

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_dvrtest_MainActivity_cameraPowerUp(JNIEnv *env, jobject thiz) {
    int fd1 = open("/dev/v4l-subdev17", O_RDWR);
    if (fd1 > 0) {
        struct sensorb_cfg_data cfg;
        cfg.cfgtype = CFG_POWER_UP;
        int ret = ioctl(fd1, VIDIOC_MSM_SENSOR_CFG, &cfg);
        if(ret < 0){
            FLOGE("ioctl power up failed! fd=%d, ret=%d", fd1, ret);
        }
        close(fd1);
    }else{
        FLOGE("open /dev/v4l-subdev17 failed!");
    }
    //int fd2 = open("/dev/v4l-subdev5", O_RDWR);
    //if (fd2 > 0) {
    //    struct csid_cfg_data cfg;
    //    cfg.cfgtype = CSID_INIT;
    //    int ret = ioctl(fd2, VIDIOC_MSM_CSID_IO_CFG, &cfg);
    //    if(ret < 0){
    //        FLOGE("ioctl csid init failed! fd=%d, ret=%d", fd2, ret);
    //    }
    //    close(fd2);
    //}else{
    //    FLOGE("open /dev/v4l-subdev5 failed!");
    //}
    int fd3 = open("/dev/v4l-subdev17", O_RDWR);
    if (fd3 > 0) {
        for (int i = 0; i < 3; i++) {
            struct sensorb_cfg_data cfg1;
            cfg1.cfgtype = CFG_WRITE_I2C_ARRAY;
            int ret = ioctl(fd3, VIDIOC_MSM_SENSOR_CFG, &cfg1);
            if (ret < 0) {
                FLOGE("ioctl power up failed! fd=%d, ret=%d", fd3, ret);
            }
        }
        close(fd3);
    }else{
        FLOGE("open /dev/v4l-subdev17 failed!");
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_dvrtest_MainActivity_cameraPowerDown(JNIEnv *env, jobject thiz) {
    int fd = open("/dev/v4l-subdev17", O_RDWR | O_NDELAY | O_NOCTTY);
    if (fd > 0) {
        struct sensorb_cfg_data cfg;
        cfg.cfgtype = CFG_POWER_DOWN;
        int ret = ioctl(fd, VIDIOC_MSM_SENSOR_CFG, &cfg);
        if(ret < 0){
            FLOGE("ioctl power down failed! fd=%d, ret=%d", fd, ret);
        }
        close(fd);
    }else{
        FLOGE("open /dev/v4l-subdev17 failed!");
    }
}