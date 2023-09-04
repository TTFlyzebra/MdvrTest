//
// Created by FlyZebra on 2021/10/02 0016.
//

#ifndef F_ZEBRA_USERCLIENT_H
#define F_ZEBRA_USERCLIENT_H

#if defined(WIN32)
#include <WinSock2.h>
#elif defined(__unix)
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <unistd.h>
#ifndef SD_BOTH
#define SD_BOTH SHUT_RDWR
#endif
#endif

#include <set>
#include "base/BaseNotify.h"
#include "base/User.h"

class UserServer;

class UserClient :public BaseNotify {
public:
    UserClient(UserServer* server, Notify* notify, int32_t socket);
    ~UserClient();
    void notify(const char* data, int32_t size) override;
    void handle(NofifyType type, const char* data, int32_t size, const char* params) override;
    int64_t getUid();

private:
    void recvThread();
    void sendThread();
    void handleData();
    void sendData(const char* data, int32_t size);
    bool connected();
    void disconnected();
    void selfFixedThread();

private:
    User U;

    UserServer* mServer;
    int32_t mSocket;

    std::thread* send_t;
    std::vector<char> sendBuf;
    std::mutex mlock_send;
    std::condition_variable mcond_send;

    std::thread* recv_t;
    std::thread* hand_t;
    std::vector<char> recvBuf;
    std::mutex mlock_recv;
    std::condition_variable mcond_recv;

    fd_set set;
    struct timeval tv;

    std::set<uint64_t> screen_set;
    std::mutex mlock_screen;

    std::set<uint64_t> sndout_set;
    std::mutex mlock_sndout;

    std::set<uint64_t> camout_set;
    std::mutex mlock_camout;

    std::set<uint64_t> micout_set;
    std::mutex mlock_micout;

    std::set<uint64_t> terminal_set;
    std::mutex mlock_terminal;   

    std::thread* fixed_t;
    int64_t lastHeartBeat;
};

#endif //F_ZEBRA_USERCLIENT_H
