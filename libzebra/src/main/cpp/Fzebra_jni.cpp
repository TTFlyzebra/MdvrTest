#include <jni.h>
#include <string>
#include "utils/FlyLog.h"
#include "Fzebra.h"

JavaVM *mJvm = nullptr;

extern "C" jint JNI_OnLoad(JavaVM *jvm, void *reserved) {
    mJvm = jvm;
    JNIEnv *env = nullptr;
    jint result = -1;
    if (jvm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        FLOGE("JNI OnLoad failed\n");
        return result;
    }
    return JNI_VERSION_1_4;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_flyzebra_core_Fzebra__1init(JNIEnv *env, jobject thiz) {
    auto *fzebra = new Fzebra(mJvm, env, thiz);
    return reinterpret_cast<jlong>(fzebra);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_core_Fzebra__1release(JNIEnv *env, jobject thiz, jlong p_obj) {
    auto *fzebra = reinterpret_cast<Fzebra *>(p_obj);
    delete fzebra;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_core_Fzebra__1setTid(JNIEnv *env, jobject thiz, jlong tid) {
    Fzebra::setTid(tid);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_core_Fzebra__1setUid(JNIEnv *env, jobject thiz, jlong uid) {
    Fzebra::setUid(uid);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_core_Fzebra__1notify(JNIEnv *env, jobject thiz, jlong p_obj, jbyteArray jdata,
                                       jint size) {
    auto *data = (const char *) env->GetByteArrayElements(jdata, JNI_FALSE);
    auto *fzebra = reinterpret_cast<Fzebra *>(p_obj);
    fzebra->nativeNotifydata(data, size);
    env->ReleaseByteArrayElements(jdata, (jbyte *) data, JNI_ABORT);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_core_Fzebra__1handle(JNIEnv *env, jobject thiz, jlong p_obj, jint type,
                                       jbyteArray jdata, jint size, jbyteArray jparams) {
    auto *data = (const char *) env->GetByteArrayElements(jdata, JNI_FALSE);
    const char *params = nullptr;
    if (jparams != nullptr) {
        params = (const char *) env->GetByteArrayElements(jparams, JNI_FALSE);
    }
    auto *fzebra = reinterpret_cast<Fzebra *>(p_obj);
    fzebra->nativeHandledata(static_cast<NofifyType>(type), data, size, params);
    env->ReleaseByteArrayElements(jdata, (jbyte *) data, JNI_ABORT);
    if (jparams != nullptr) {
        env->ReleaseByteArrayElements(jparams, (jbyte *) params, JNI_ABORT);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_core_Fzebra__1startUserServer(JNIEnv *env, jobject thiz, jlong p_obj) {
    auto *fzebra = reinterpret_cast<Fzebra *>(p_obj);
    fzebra->startUserServer();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_core_Fzebra__1stopUserServer(JNIEnv *env, jobject thiz, jlong p_obj) {
    auto *fzebra = reinterpret_cast<Fzebra *>(p_obj);
    fzebra->stopUserServer();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_core_Fzebra__1startUserSession(JNIEnv *env, jobject thiz, jlong p_obj, jlong uid,
                                                 jstring jsip) {
    auto *fzebra = reinterpret_cast<Fzebra *>(p_obj);
    const char *sip = env->GetStringUTFChars(jsip, 0);
    fzebra->startUserSession(uid, sip);
    env->ReleaseStringUTFChars(jsip, sip);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_core_Fzebra__1stopUserSession(JNIEnv *env, jobject thiz, jlong p_obj,
                                                jstring jsip) {
    auto *fzebra = reinterpret_cast<Fzebra *>(p_obj);
    const char *sip = env->GetStringUTFChars(jsip, 0);
    fzebra->stopUserSession(sip);
    env->ReleaseStringUTFChars(jsip, sip);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_core_Fzebra__1startRtspServer(JNIEnv *env, jobject thiz, jlong p_obj) {
    auto *fzebra = reinterpret_cast<Fzebra *>(p_obj);
    fzebra->startRtspServer();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_core_Fzebra__1stopRtspServer(JNIEnv *env, jobject thiz, jlong p_obj) {
    auto *fzebra = reinterpret_cast<Fzebra *>(p_obj);
    fzebra->stopRtspServer();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_core_Fzebra__1startScreenServer(JNIEnv *env, jobject thiz, jlong p_obj,
                                                  jlong tid) {
    auto *fzebra = reinterpret_cast<Fzebra *>(p_obj);
    fzebra->startScreenServer(tid);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_core_Fzebra__1stopScreenServer(JNIEnv *env, jobject thiz, jlong p_obj,
                                                 jlong tid) {
    auto *fzebra = reinterpret_cast<Fzebra *>(p_obj);
    fzebra->stopScreenServer(tid);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_core_Fzebra__1startSndoutServer(JNIEnv *env, jobject thiz, jlong p_obj,
                                                  jlong tid) {
    auto *fzebra = reinterpret_cast<Fzebra *>(p_obj);
    fzebra->startSndoutServer(tid);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_core_Fzebra__1stopSndoutServer(JNIEnv *env, jobject thiz, jlong p_obj,
                                                 jlong tid) {
    auto *fzebra = reinterpret_cast<Fzebra *>(p_obj);
    fzebra->stopSndoutServer(tid);
}