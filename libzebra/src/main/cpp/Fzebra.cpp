//
// Created by Administrator on 2023/6/30.
//

#include <stdint.h>
#include "Fzebra.h"
#include "rfc/Protocol.h"

Fzebra::Fzebra(JavaVM *jvm, JNIEnv *env, jobject thiz) {
    N = new Notify();
    N->registerListener(this);
    cb = new FzebraCB(jvm, env, thiz);
}

Fzebra::~Fzebra() {
    N->unregisterListener(this);
    delete N;
    delete cb;
}

void Fzebra::notify(const char *data, int32_t size) {
    N->loghex(data, size > 20 ? 20 : size, "[NOTIFY]");
    auto *notifyData = (NotifyData *) data;
    switch (notifyData->type) {
        break;
    }
}

void Fzebra::handle(NofifyType type, const char *data, int32_t size, const char *params) {
    N->loghex(data, size > 20 ? 20 : size, "[HANDLE]");
    switch (type) {
        break;
    }
}

void Fzebra::nativeNotifydata(const char *data, int32_t size) {
    N->notifydata(data, size);
}

void Fzebra::nativeHandledata(NofifyType type, const char *data, int32_t size, const char *parmas) {
    N->handledata(type, data, size, parmas);
}
