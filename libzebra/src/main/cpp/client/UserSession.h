#ifndef ANDROID_USERSESSION_H
#define ANDROID_USERSESSION_H

#include "base/BaseNotify.h"

class UserSession : public BaseNotify {
public:
    UserSession(Notify* notify, int64_t uid, const char* sip);
    ~UserSession();
    void notify(const char* data, int32_t size) override;

private:
    void connThread();
    void sendThread();
    void handleData();
    void selfFixedThread();
    void sendData(const char* data, int32_t size);
    void connected();
    void disconnected();

private:
    int64_t mUid;
    char mSvIP[20];
    int32_t mSocket;
    std::mutex mlock_conn;
    volatile bool is_connect;
    std::condition_variable mcond_conn;

    std::thread* send_t;
    std::mutex mlock_send;
    std::vector<char> sendBuf;
    std::condition_variable mcond_send;

    std::thread* recv_t;
    std::thread* hand_t;
    std::mutex mlock_recv;
    std::vector<char> recvBuf;
    std::condition_variable mcond_recv;

    fd_set set;
    struct timeval tv;

    std::thread* time_t;
};

#endif //ANDROID_USERSESSION_H
