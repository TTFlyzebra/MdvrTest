//
// Created by Administrator on 2023/6/23.
//

#include "FzebraCB.h"
#include "utils/FlyLog.h"

FzebraCB::FzebraCB(JavaVM *jvm, JNIEnv *env, jobject thiz) {
//    FLOGI("%s()", __func__);
    mjvm = jvm;
    mEnv = env;
    mThiz = mEnv->NewGlobalRef(thiz);
    jclass cls = mEnv->GetObjectClass(mThiz);
    if (!cls) {
        FLOGE("find jclass faild");
        return;
    }
    notifydata = mEnv->GetMethodID(cls, "javaNotifydata", "([BI)V");
    handledata = mEnv->GetMethodID(cls, "javaHandleData", "(I[BI[B)V");
    mEnv->DeleteLocalRef(cls);
}

FzebraCB::~FzebraCB() {
    int status = mjvm->GetEnv((void **) &mEnv, JNI_VERSION_1_4);
    bool isAttacked = false;
    if (status < 0) {
        status = mjvm->AttachCurrentThread(&mEnv, nullptr);
        if (status < 0) {
            FLOGE("onVideoEncode: failed to attach current thread");
            return;
        }
        isAttacked = true;
    }
    mEnv->DeleteGlobalRef(mThiz);
    if (isAttacked) {
        (mjvm)->DetachCurrentThread();
    }
    FLOGI("%s()", __func__);
}

void FzebraCB::javaNotifydata(const char *data, int32_t size) {
    int status = mjvm->GetEnv((void **) &mEnv, JNI_VERSION_1_4);
    bool isAttacked = false;
    if (status < 0) {
        status = mjvm->AttachCurrentThread(&mEnv, nullptr);
        if (status < 0) {
            FLOGE("jni notify: failed to attach current thread");
            return;
        }
        isAttacked = true;
    }
    mEnv->CallVoidMethod(mThiz, notifydata, data, size);
    if (isAttacked) {
        (mjvm)->DetachCurrentThread();
    }
}

void FzebraCB::javaHandledata(int32_t type, const char *data, int32_t size, const char *parmas) {
    int status = mjvm->GetEnv((void **) &mEnv, JNI_VERSION_1_4);
    bool isAttacked = false;
    if (status < 0) {
        status = mjvm->AttachCurrentThread(&mEnv, nullptr);
        if (status < 0) {
            FLOGE("jni handle: failed to attach current thread");
            return;
        }
        isAttacked = true;
    }
    mEnv->CallVoidMethod(mThiz, handledata, type, data, size, parmas);
    if (isAttacked) {
        (mjvm)->DetachCurrentThread();
    }
}
