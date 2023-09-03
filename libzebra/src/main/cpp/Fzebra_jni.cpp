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
    auto *params = (const char *) env->GetByteArrayElements(jparams, JNI_FALSE);
    auto *fzebra = reinterpret_cast<Fzebra *>(p_obj);
    fzebra->nativeHandledata(static_cast<NofifyType>(type), data, size, params);
    env->ReleaseByteArrayElements(jdata, (jbyte *) data, JNI_ABORT);
    env->ReleaseByteArrayElements(jparams, (jbyte *) params, JNI_ABORT);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_core_Fzebra__1enableRtspServer(JNIEnv *env, jobject thiz, jlong p_obj) {
    auto *fzebra = reinterpret_cast<Fzebra *>(p_obj);
    fzebra->startRtspServer();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_core_Fzebra__1disableRtspServer(JNIEnv *env, jobject thiz, jlong p_obj) {
    auto *fzebra = reinterpret_cast<Fzebra *>(p_obj);
    fzebra->stopRtspServer();
}