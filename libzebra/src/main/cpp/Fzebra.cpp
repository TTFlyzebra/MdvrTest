//
// Created by Administrator on 2023/6/30.
//

#include <stdint.h>
#include "Fzebra.h"
#include "buffer/BufferManager.h"
#include "rfc/Protocol.h"
#include "utils/FlyLog.h"
#include "server/UserServer.h"
#include "client/UserSession.h"
#include "rtsp/RtspServer.h"
#include "service/SndOutService.h"
#include "service/ScreenService.h"
#include "utils/ByteUtil.h"
#include "base/Global.h"

Fzebra::Fzebra(JavaVM *jvm, JNIEnv *env, jobject thiz)
: mUserServer(nullptr)
, mRtspServer(nullptr) {
    cb = new FzebraCB(jvm, env, thiz);
    BufferManager::get()->init();
    T = new Terminal();
    U = new User();
    N = new Notify();
    N->registerListener(this);
}

Fzebra::~Fzebra() {
    stopUserServer();
    startRtspServer();
    for (auto item: mUserSessions) {
        char sn[16] = {0};
        delete item.second;
    }
    mUserSessions.clear();
    for (auto item: mScreens) {
        char sn[16] = {0};
        delete item.second;
    }
    mScreens.clear();
    for (auto item: mSndOuts) {
        char sn[16] = {0};
        delete item.second;
    }
    mSndOuts.clear();

    N->unregisterListener(this);
    delete cb;
    delete T;
    delete U;
    delete N;
    //Notify使用了BufferMangaer,所以要在Notify后面释放
    BufferManager::get()->release();
}

void Fzebra::notify(const char *data, int32_t size) {
    auto *notifyData = (NotifyData *) data;
    switch (notifyData->type) {
        case TYPE_U_CONNECTED:
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

void Fzebra::handle(NofifyType type, const char *data, int32_t dsize, const char *params, int32_t psize) {
    switch (type) {
        break;
    }
}

void Fzebra::nativeNotifydata(const char *data, int32_t size) {
    N->notifydata(data, size);
}

void Fzebra::nativeHandledata(NofifyType type, const char *data, int32_t dsize, const char *parmas, int32_t psize) {
    N->handledata(type, data, dsize, parmas, psize);
}

void Fzebra::setTid(int64_t tid) {
    T->tid = tid;
}

void Fzebra::setUid(int64_t uid) {
    U->uid = uid;
}

void Fzebra::startUserServer() {
    mUserServer = new UserServer(N);
}

void Fzebra::stopUserServer() {
    if (mUserServer) {
        delete mUserServer;
        mUserServer = nullptr;
    }
}

void Fzebra::startUserSession(int64_t uid, const char *sip) {
    uint32_t id = inet_addr(sip);
    auto it = mUserSessions.find(id);
    if (it == mUserSessions.end()) {
        auto *userSession = new UserSession(N, uid, sip);
        FLOGI("UserlSession connect ip %s", sip);
        mUserSessions.emplace(id, userSession);
    }
}

void Fzebra::stopUserSession(const char *sip) {
    uint32_t id = inet_addr(sip);
    auto it = mUserSessions.find(id);
    if (it != mUserSessions.end()) {
        delete it->second;
    }
    mScreens.erase(id);
}

void Fzebra::startRtspServer() {
    mRtspServer = new RtspServer(N);
}

void Fzebra::stopRtspServer() {
    if (mRtspServer) {
        delete mRtspServer;
        mRtspServer = nullptr;
    }
}

void Fzebra::startScreenServer(int64_t tid) {
    auto it = mScreens.find(tid);
    if (it == mScreens.end()) {
        auto *screenService = new ScreenService(N, tid);
        mScreens.emplace(tid, screenService);
    }
}

void Fzebra::stopScreenServer(int64_t tid) {
    auto it = mScreens.find(tid);
    if (it != mScreens.end()) {
        delete it->second;
    }
    mScreens.erase(tid);
}

void Fzebra::startSndoutServer(int64_t tid) {
    auto it = mSndOuts.find(tid);
    if (it == mSndOuts.end()) {
        auto *sndOutService = new SndOutService(N, tid);
        mSndOuts.emplace(tid, sndOutService);
    }
}

void Fzebra::stopSndoutServer(int64_t tid) {
    auto it = mSndOuts.find(tid);
    if (it != mSndOuts.end()) {
        delete it->second;
    }
    mSndOuts.erase(tid);
}

