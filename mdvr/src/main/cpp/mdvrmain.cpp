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
#define CFG_POWER_UP                  47
#define CFG_POWER_DOWN                48

enum camb_position_t {
    BACK_CAMERA_B,
    FRONT_CAMERA_B,
    AUX_CAMERA_B = 0x100,
    INVALID_CAMERA_B,
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

#define VIDIOC_MSM_SENSOR_CFG \
	_IOWR('V', BASE_VIDIOC_PRIVATE + 1, struct sensorb_cfg_data)

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_dvrtest_MainActivity_cameraPowerUp(JNIEnv *env, jobject thiz) {
    struct sensorb_cfg_data cfg;
    cfg.cfgtype = CFG_POWER_UP;
    int fd = open("/dev/rn6864m_csi1", O_RDWR | O_NDELAY | O_NOCTTY);
    if (fd > 0) {
        int ret = ioctl(fd, VIDIOC_MSM_SENSOR_CFG, &cfg);
        if(ret < 0){
            FLOGE("ioctl power up failed! fd=%d, ret=%d", fd, ret);
        }
        close(fd);
    }else{
        FLOGE("open /dev/rn6864m_csi1 failed!");
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_dvrtest_MainActivity_cameraPowerDown(JNIEnv *env, jobject thiz) {
    struct sensorb_cfg_data cfg;
    cfg.cfgtype = CFG_POWER_DOWN;
    int fd = open("/dev/rn6864m_csi1", O_RDWR | O_NDELAY | O_NOCTTY);
    if (fd > 0) {
        int ret = ioctl(fd, VIDIOC_MSM_SENSOR_CFG, &cfg);
        if(ret < 0){
            FLOGE("ioctl power down failed! ret=%d", ret);
        }
        close(fd);
    }else{
        FLOGE("open /dev/rn6864m_csi1 failed!");
    }
}