//
// Created by Administrator on 2023/6/23.
//

#include "FzebraCB.h"
#include "utils/FlyLog.h"

FzebrCB::FzebrCB(JavaVM *jvm, JNIEnv *env, jobject thiz) {
//    FLOGI("%s()", __func__);
    javeVM = jvm;
    jniEnv = env;
    jObject = jniEnv->NewGlobalRef(thiz);
    jclass cls = jniEnv->GetObjectClass(jObject);
    if (!cls) {
        FLOGE("find jclass faild");
        return;
    }
    notify = jniEnv->GetMethodID(cls, "notify", "([BI])V");
    handle = jniEnv->GetMethodID(cls, "handle", "(I[BI[B)V");
    jniEnv->DeleteLocalRef(cls);
}

FzebrCB::~FzebrCB() {
    int status = javeVM->GetEnv((void **) &jniEnv, JNI_VERSION_1_6);
    bool isAttacked = false;
    if (status < 0) {
        status = javeVM->AttachCurrentThread(&jniEnv, nullptr);
        if (status < 0) {
            FLOGE("onVideoEncode: failed to attach current thread");
            return;
        }
        isAttacked = true;
    }
    jniEnv->DeleteGlobalRef(jObject);
    if (isAttacked) {
        (javeVM)->DetachCurrentThread();
    }
//    FLOGI("%s()", __func__);
}

void FzebrCB::javaNotify(const char *data, int32_t size) {
    int status = javeVM->GetEnv((void **) &jniEnv, JNI_VERSION_1_6);
    bool isAttacked = false;
    if (status < 0) {
        status = javeVM->AttachCurrentThread(&jniEnv, nullptr);
        if (status < 0) {
            FLOGE("jni notify: failed to attach current thread");
            return;
        }
        isAttacked = true;
    }
    jniEnv->CallVoidMethod(jObject, notify, data, size);
    if (isAttacked) {
        (javeVM)->DetachCurrentThread();
    }
}

void FzebrCB::javaHandle(int32_t type, const char *data, int32_t size, const char *parmas) {
    int status = javeVM->GetEnv((void **) &jniEnv, JNI_VERSION_1_6);
    bool isAttacked = false;
    if (status < 0) {
        status = javeVM->AttachCurrentThread(&jniEnv, nullptr);
        if (status < 0) {
            FLOGE("jni handle: failed to attach current thread");
            return;
        }
        isAttacked = true;
    }
    jniEnv->CallVoidMethod(jObject, handle, type, data, size, parmas);
    if (isAttacked) {
        (javeVM)->DetachCurrentThread();
    }
}
