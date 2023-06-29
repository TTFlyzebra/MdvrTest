#include <jni.h>
#include <cstring>
#include <malloc.h>

extern "C" {
#include <librtmp/rtmp.h>
}

#include "utils/FlyLog.h"
#include "RtmpDump.h"

JavaVM *javaVM = nullptr;

extern "C" jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVM = vm;
    JNIEnv *env = nullptr;
    jint result = -1;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        FLOGE("JNI OnLoad failed\n");
        return result;
    }
    return JNI_VERSION_1_4;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_flyzebra_rtmp_RtmpDump__1open(JNIEnv *env, jobject thiz, jint channel, jstring jurl) {
    const char *url = env->GetStringUTFChars(jurl, JNI_FALSE);
    auto *rtmpDump = new RtmpDump(javaVM, env, thiz);
    bool ret = rtmpDump->open(channel, url);
    env->ReleaseStringUTFChars(jurl, url);
    return ret ? reinterpret_cast<jlong>(rtmpDump) : -1;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_rtmp_RtmpDump__1close(JNIEnv *env, jobject thiz, jlong p_obj) {
    RtmpDump *rtmpDump = reinterpret_cast<RtmpDump *>(p_obj);
    rtmpDump->close();
    delete rtmpDump;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_flyzebra_rtmp_RtmpDump__1writeAacHead(JNIEnv *env, jobject thiz, jlong p_obj,
                                               jbyteArray jhead, jint headLen) {
    const char *head = reinterpret_cast<const char *>(env->GetByteArrayElements(jhead, JNI_FALSE));
    RtmpDump *rtmpDump = reinterpret_cast<RtmpDump *>(p_obj);
    jboolean ret = rtmpDump->sendAacHead(head, headLen);
    env->ReleaseByteArrayElements(jhead, (jbyte *) head, JNI_ABORT);
    return ret;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_flyzebra_rtmp_RtmpDump__1writeAacData(JNIEnv *env, jobject thiz, jlong p_obj,
                                               jbyteArray jdata,
                                               jint size, jlong pts) {
    const char *data = reinterpret_cast<const char *>(env->GetByteArrayElements(jdata, JNI_FALSE));
    RtmpDump *rtmpDump = reinterpret_cast<RtmpDump *>(p_obj);
    jboolean ret = rtmpDump->sendAacData(data, size, pts);
    env->ReleaseByteArrayElements(jdata, (jbyte *) data, JNI_ABORT);
    return ret;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_flyzebra_rtmp_RtmpDump__1writeAvcHead(JNIEnv *env, jobject thiz, jlong p_obj,
                                               jbyteArray jdata, jint size) {
    const char *data = reinterpret_cast<const char *>(env->GetByteArrayElements(jdata, JNI_FALSE));
    RtmpDump *rtmpDump = reinterpret_cast<RtmpDump *>(p_obj);
    jboolean ret = rtmpDump->sendAvcHead(data, size);
    env->ReleaseByteArrayElements(jdata, (jbyte *) data, JNI_ABORT);
    return ret;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_flyzebra_rtmp_RtmpDump__1writeAvcData(JNIEnv *env, jobject thiz, jlong p_obj,
                                               jbyteArray jdata,
                                               jint size, jlong pts) {
    const char *data = reinterpret_cast<const char *>(env->GetByteArrayElements(jdata, JNI_FALSE));
    RtmpDump *rtmpDump = reinterpret_cast<RtmpDump *>(p_obj);
    jboolean ret = rtmpDump->sendAvcData(data, size, pts);
    env->ReleaseByteArrayElements(jdata, (jbyte *) data, JNI_ABORT);
    return ret;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_flyzebra_rtmp_RtmpDump__1writeHevcHead(JNIEnv *env, jobject thiz, jlong p_obj,
                                                jbyteArray jdata, jint size) {
    const char *data = reinterpret_cast<const char *>(env->GetByteArrayElements(jdata, JNI_FALSE));
    RtmpDump *rtmpDump = reinterpret_cast<RtmpDump *>(p_obj);
    jboolean ret = rtmpDump->sendHevcHead(data, size);
    env->ReleaseByteArrayElements(jdata, (jbyte *) data, JNI_ABORT);
    return ret;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_flyzebra_rtmp_RtmpDump__1writeHevcData(JNIEnv *env, jobject thiz, jlong p_obj,
                                                jbyteArray jdata,
                                                jint size, jlong pts) {
    const char *data = reinterpret_cast<const char *>(env->GetByteArrayElements(jdata, JNI_FALSE));
    RtmpDump *rtmpDump = reinterpret_cast<RtmpDump *>(p_obj);
    jboolean ret = rtmpDump->sendHevcData(data, size, pts);
    env->ReleaseByteArrayElements(jdata, (jbyte *) data, JNI_ABORT);
    return ret;
}
