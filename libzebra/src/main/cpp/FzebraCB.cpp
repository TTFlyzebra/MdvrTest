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
    std::lock_guard<std::mutex> lock(mlock_call);
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
    jbyteArray jdata = mEnv->NewByteArray(static_cast<jsize>(size));
    mEnv->SetByteArrayRegion(jdata, 0, size, reinterpret_cast<const jbyte *>(data));
    mEnv->CallVoidMethod(mThiz, notifydata, jdata, size);
    mEnv->DeleteLocalRef(jdata);
    if (isAttacked) {
        (mjvm)->DetachCurrentThread();
    }
}

void FzebraCB::javaHandledata(int32_t type, const char *data, int32_t dataLen, const char *parmas, int32_t parmasLen) {
    std::lock_guard<std::mutex> lock(mlock_call);
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
    jbyteArray jdata = mEnv->NewByteArray(static_cast<jsize>(dataLen));
    mEnv->SetByteArrayRegion(jdata, 0, dataLen, reinterpret_cast<const jbyte *>(data));
    jbyteArray jparmas = mEnv->NewByteArray(static_cast<jsize>(parmasLen));
    mEnv->SetByteArrayRegion(jparmas, 0, parmasLen, reinterpret_cast<const jbyte *>(parmas));
    mEnv->CallVoidMethod(mThiz, handledata, type, jdata, dataLen, jparmas);
    mEnv->DeleteLocalRef(jdata);
    mEnv->DeleteLocalRef(jparmas);
    if (isAttacked) {
        (mjvm)->DetachCurrentThread();
    }
}
