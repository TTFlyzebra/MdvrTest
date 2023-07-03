//
// Created by Administrator on 2023/6/30.
//

#include <stdint.h>
#include "Fzebra.h"
#include "buffer/BufferManager.h"
#include "rfc/Protocol.h"
#include "rtsp/RtspServer.h"

Fzebra::Fzebra(JavaVM *jvm, JNIEnv *env, jobject thiz) {
    cb = new FzebraCB(jvm, env, thiz);
    BufferManager::get()->init();
    N = new Notify();
    N->registerListener(this);
}

Fzebra::~Fzebra() {
    N->unregisterListener(this);
    delete cb;

    delete N;
    //Notify使用了BufferMangaer,所以要在Notify后面释放
    BufferManager::get()->release();
}

void Fzebra::notify(const char *data, int32_t size) {
    auto *notifyData = (NotifyData *) data;
    switch (notifyData->type) {
        break;
    }
}

void Fzebra::handle(NofifyType type, const char *data, int32_t size, const char *params) {
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

void Fzebra::startRtspServer() {
    rtsp = new RtspServer(N);
}

void Fzebra::stopRtspServer() {
    delete rtsp;
}
