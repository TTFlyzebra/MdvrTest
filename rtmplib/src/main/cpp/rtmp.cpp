#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jlong JNICALL
Java_com_flyzebra_rtmplib_RtmpPushHelper_openRtmpPushUrl(JNIEnv *env, jobject thiz, jstring url) {
    // TODO: implement openRtmpUrl()
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_flyzebra_rtmplib_RtmpPushHelper_pushSpsPps(JNIEnv *env, jobject thiz, jlong id, jbyteArray sps,
                                                    jint sps_len, jbyteArray pps, jint pps_len) {
    // TODO: implement pushSpsPps()
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_flyzebra_rtmplib_RtmpPushHelper_pushSpsPpsVps(JNIEnv *env, jobject thiz, jlong id,
                                                       jbyteArray sps, jint sps_len, jbyteArray pps,
                                                       jint pps_len, jbyteArray vps, jint vps_len) {
    // TODO: implement pushSpsPpsVps()
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_flyzebra_rtmplib_RtmpPushHelper_pushVideoFrame(JNIEnv *env, jobject thiz, jlong id,
                                                        jbyteArray data, jint data_len,
                                                        jboolean key_frame, jboolean is_h264) {
    // TODO: implement pushVideoFrame()
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_flyzebra_rtmplib_RtmpPushHelper_pushAudioHeader(JNIEnv *env, jobject thiz, jlong id,
                                                         jbyteArray data, jint data_len) {
    // TODO: implement pushAudioHeader()
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_flyzebra_rtmplib_RtmpPushHelper_pushAudioFrame(JNIEnv *env, jobject thiz, jlong id,
                                                        jbyteArray data, jint data_len) {
    // TODO: implement pushAudioFrame()
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_rtmplib_RtmpPushHelper_closeRtmpPush(JNIEnv *env, jobject thiz, jlong id) {
    // TODO: implement closeRtmp()
}