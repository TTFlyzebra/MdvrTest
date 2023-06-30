//
// Created by Administrator on 2023/6/23.
//

#ifndef FZEBRA_CALLBACK_H
#define FZEBRA_CALLBACK_H

#include <jni.h>

class FzebrCB {
public:
    FzebrCB(JavaVM* jvm, JNIEnv *env, jobject thiz);
    ~FzebrCB();

    void javaNotify(const char* data, int32_t size);
    void javaHandle(int32_t type, const char* data, int32_t size, const char* parmas);

private:
    JavaVM* javeVM ;
    JNIEnv *jniEnv ;
    jobject jObject;

    jmethodID notify;
    jmethodID handle;
};


#endif //FZEBRA_CALLBACK_H
