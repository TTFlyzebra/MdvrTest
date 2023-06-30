//
// Created by Administrator on 2023/6/23.
//

#include "RtmpDumpCB.h"
#include "utils/FlyLog.h"

RtmpDumpCB::RtmpDumpCB(JavaVM *jvm, JNIEnv *env, jobject thiz) {
//    FLOGI("%s()", __func__);
    mJvm = jvm;
    mEnv = env;
    mThiz = mEnv->NewGlobalRef(thiz);
    jclass cls = mEnv->GetObjectClass(mThiz);
    if (!cls) {
        FLOGE("find jclass faild");
        return;
    }
    onError = mEnv->GetMethodID(cls, "onError", "(I)V");
    mEnv->DeleteLocalRef(cls);
}

RtmpDumpCB::~RtmpDumpCB() {
    int status = mJvm->GetEnv((void **) &mEnv, JNI_VERSION_1_4);
    bool isAttacked = false;
    if (status < 0) {
        status = mJvm->AttachCurrentThread(&mEnv, nullptr);
        if (status < 0) {
            FLOGE("onVideoEncode: failed to attach current thread");
            return;
        }
        isAttacked = true;
    }
    mEnv->DeleteGlobalRef(mThiz);
    if (isAttacked) {
        (mJvm)->DetachCurrentThread();
    }
//    FLOGI("%s()", __func__);
}

void RtmpDumpCB::javaOnError(int error) {
    int status = mJvm->GetEnv((void **) &mEnv, JNI_VERSION_1_4);
    bool isAttacked = false;
    if (status < 0) {
        status = mJvm->AttachCurrentThread(&mEnv, nullptr);
        if (status < 0) {
            FLOGE("onStop: failed to attach current thread");
            return;
        }
        isAttacked = true;
    }
    mEnv->CallVoidMethod(mThiz, onError, error);
    if (isAttacked) {
        (mJvm)->DetachCurrentThread();
    }
}
