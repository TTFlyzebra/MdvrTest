#include <jni.h>
#include <string>
#include "utils/FlyLog.h"
#include "Fzebra.h"

JavaVM *javaVM = nullptr;

extern "C" jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVM = vm;
    JNIEnv *env = nullptr;
    jint result = -1;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        FLOGE("JNI OnLoad failed\n");
        return result;
    }
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_flyzebra_core_Fzebra__1init(JNIEnv *env, jobject thiz) {
    auto *fzebra = new Fzebra();
    return reinterpret_cast<jlong>(fzebra);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_core_Fzebra__1release(JNIEnv *env, jobject thiz, jlong p_obj) {
    Fzebra *fzebra = reinterpret_cast<Fzebra *>(p_obj);
    delete fzebra;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_core_Fzebra__1notify(JNIEnv *env, jobject thiz, jlong p_obj, jbyteArray data,
                                       jint size) {
    // TODO: implement _notify()
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_core_Fzebra__1handle(JNIEnv *env, jobject thiz, jlong p_obj, jint type,
                                       jbyteArray data, jint size, jbyteArray params) {
    // TODO: implement _handle()
}