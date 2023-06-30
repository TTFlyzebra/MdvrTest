//
// Created by Administrator on 2023/6/30.
//

#ifndef MDVRTEST_FZEBRA_H
#define MDVRTEST_FZEBRA_H


#include "base/Notify.h"
#include "FzebraCB.h"

class Fzebra : public INotify{
public:
    Fzebra(JavaVM* jvm, JNIEnv *env, jobject thiz);

    ~Fzebra();

    void notify(const char* data, int32_t size) override;

    void handle(NofifyType type, const char* data, int32_t size, const char *params) override;

    void nativeNotifydata(const char *data, int32_t size);

    void nativeHandledata(NofifyType type, const char *data, int32_t size, const char *params);

private:
    Notify* N;
    FzebraCB* cb;
};


#endif //MDVRTEST_FZEBRA_H
