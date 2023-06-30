//
// Created by Administrator on 2023/6/23.
//

#ifndef RTMPCAMERA_CALLBACK_H
#define RTMPCAMERA_CALLBACK_H

#include <jni.h>

class RtmpDumpCB {
public:
    RtmpDumpCB(JavaVM* jvm, JNIEnv *env, jobject thiz);
    ~RtmpDumpCB();
    void javaOnError(int error);

private:
    JavaVM* javeVM ;
    JNIEnv *jniEnv ;
    jobject jObject;
    jmethodID onError;
};


#endif //RTMPCAMERA_CALLBACK_H
