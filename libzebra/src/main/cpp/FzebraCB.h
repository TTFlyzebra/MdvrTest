//
// Created by Administrator on 2023/6/23.
//

#ifndef FZEBRA_CALLBACK_H
#define FZEBRA_CALLBACK_H

#include <jni.h>

class FzebraCB {
public:
    FzebraCB(JavaVM* jvm, JNIEnv *env, jobject thiz);
    ~FzebraCB();

    void javaNotifydata(const char* data, int32_t size);
    void javaHandledata(int32_t type, const char* data, int32_t size, const char* parmas);

private:
    JavaVM* javeVM ;
    JNIEnv *jniEnv ;
    jobject jObject;

    jmethodID notifydata;
    jmethodID handledata;
};


#endif //FZEBRA_CALLBACK_H
