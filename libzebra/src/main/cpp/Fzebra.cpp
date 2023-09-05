//
// Created by Administrator on 2023/6/30.
//

#include <stdint.h>
#include "Fzebra.h"
#include "buffer/BufferManager.h"
#include "rfc/Protocol.h"
#include "server/UserServer.h"
#include "rtsp/RtspServer.h"
#include "utils/ByteUtil.h"
#include "utils/FlyLog.h"
#include "base/Global.h"

Fzebra::Fzebra(JavaVM *jvm, JNIEnv *env, jobject thiz) {
    cb = new FzebraCB(jvm, env, thiz);
    BufferManager::get()->init();
    T = new Terminal();
    N = new Notify();
    N->registerListener(this);
}

Fzebra::~Fzebra() {
    N->unregisterListener(this);
    delete cb;
    delete T;
    delete N;
    //Notify使用了BufferMangaer,所以要在Notify后面释放
    BufferManager::get()->release();
}

void Fzebra::notify(const char *data, int32_t size) {
    auto *notifyData = (NotifyData *) data;
    switch (notifyData->type) {
        case TYPE_T_CONNECTED:
        case TYPE_U_CONNECTED:
        case TYPE_T_DISCONNECTED:
        case TYPE_U_DISCONNECTED:
        case TYPE_UT_HEARTBEAT:
        case TYPE_MCTL_REBOOT:
        case TYPE_SCREEN_U_READY:
        case TYPE_SCREEN_U_START:
        case TYPE_SCREEN_U_STOP:
        case TYPE_INPUT_TOUCH_SINGLE:
        case TYPE_INPUT_KEY_SINGLE:
        case TYPE_INPUT_TEXT_SINGLE:
        case TYPE_INPUT_TOUCH_MULTI:
        case TYPE_INPUT_KEY_MULTI:
        case TYPE_INPUT_TEXT_MULTI:
            cb->javaNotifydata(data, size);
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

void Fzebra::setTid(int64_t tid) {
    T->tid = tid;
}

void Fzebra::startUserServer() {
    user = new UserServer(N);
}

void Fzebra::stopUserServer() {
    delete user;
}

void Fzebra::startRtspServer() {
    rtsp = new RtspServer(N);
}

void Fzebra::stopRtspServer() {
    delete rtsp;
}

