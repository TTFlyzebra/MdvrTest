//
// Created by Administrator on 2023/6/30.
//

#ifndef MDVRTEST_FZEBRA_H
#define MDVRTEST_FZEBRA_H


#include "base/Notify.h"
#include "FzebraCB.h"
#include <map>

class UserServer;

class UserSession;

class RtspServer;

class SndOutService;

class ScreenService;

class Fzebra : public INotify {
public:
    Fzebra(JavaVM *jvm, JNIEnv *env, jobject thiz);

    ~Fzebra();

    void notify(const char *data, int32_t size) override;

    void handle(NofifyType type, const char *data, int32_t size, const char *params) override;

    void nativeNotifydata(const char *data, int32_t size);

    void nativeHandledata(NofifyType type, const char *data, int32_t size, const char *params);

    static void setTid(int64_t tid);

    static void setUid(int64_t uid);

    void startUserServer();

    void stopUserServer();

    void startUserSession(int64_t uid, const char *sip);

    void stopUserSession(const char *sip);

    void startRtspServer();

    void stopRtspServer();

    void startScreenServer(int64_t tid);

    void stopScreenServer(int64_t tid);

    void startSndoutServer(int64_t tid);

    void stopSndoutServer(int64_t tid);

private:
    Notify *N;
    FzebraCB *cb;
    UserServer *mUserServer;
    RtspServer *mRtspServer;
    std::map<uint32_t, UserSession *> mUserSessions;
    std::map<int64_t, SndOutService *> mSndOuts;
    std::map<int64_t, ScreenService *> mScreens;
};


#endif //MDVRTEST_FZEBRA_H
