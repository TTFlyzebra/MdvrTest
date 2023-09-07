//
// Created by FlyZebra on 2021/9/30 0030.
//
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <unistd.h>
#include "UserSession.h"
#include "Config.h"
#include "rfc/Protocol.h"
#include "base/User.h"
#include "utils/ByteUtil.h"
#include "utils/SysUtil.h"
#include "utils/FlyLog.h"
#include "utils/TimeUtil.h"

UserSession::UserSession(Notify* notify, int64_t uid, const char* sip)
    : BaseNotify(notify)
    , mUid(uid)
    , mSocket(-1)
    , is_connect(false)
{
    FLOGD("%s()", __func__);
    sprintf(mSvIP, "%s", sip);
    {
        N->registerListener(this);
        std::lock_guard<std::mutex> lock_stop(mlock_stop);
        is_stop = false;
    }
    recv_t = new std::thread(&UserSession::connThread, this);
    send_t = new std::thread(&UserSession::sendThread, this);
    hand_t = new std::thread(&UserSession::handleData, this);
    time_t = new std::thread(&UserSession::selfFixedThread, this);
}

UserSession::~UserSession()
{
    {
        N->unregisterListener(this);
        std::lock_guard<std::mutex> lock_stop(mlock_stop);
        is_stop = true;
    }
    {
        std::lock_guard<std::mutex> lock_send(mlock_send);
        if (!sendBuf.empty() && is_connect) send(mSocket, &sendBuf[0], sendBuf.size(), 0);
        sendBuf.clear();
    }
    {
        std::lock_guard<std::mutex> lock_conn(mlock_conn);
        mcond_conn.notify_all();
    }
    {
        std::lock_guard<std::mutex> lock_send(mlock_send);
        mcond_send.notify_all();
    }
    {
        std::lock_guard<std::mutex> lock_recv(mlock_recv);
        mcond_recv.notify_all();
    }

    shutdown(mSocket, SHUT_RDWR);
    close(mSocket);

    recv_t->join();
    send_t->join();
    hand_t->join();
    time_t->join();
    delete recv_t;
    delete send_t;
    delete hand_t;
    delete time_t;
    FLOGD("%s()", __func__);
}

void UserSession::notify(const char* data, int32_t size)
{
    std::lock_guard<std::mutex> lock_stop(mlock_stop);
    if(is_stop) return;
    auto * notifyData = (NotifyData*)data;
    switch (notifyData->type) {
    case TYPE_UT_HEARTBEAT:
    case TYPE_U_LOGIN:
    case TYPE_USER_TID_ADD:
    case TYPE_USER_TID_REMOVE:
    case TYPE_SCREEN_U_READY:
    case TYPE_SCREEN_U_START:
    case TYPE_SCREEN_U_STOP:
    case TYPE_SNDOUT_U_READY:
    case TYPE_SNDOUT_U_START:
    case TYPE_SNDOUT_U_STOP:
    case TYPE_CAMOUT_U_READY:
    case TYPE_CAMOUT_U_START:
    case TYPE_CAMOUT_U_STOP:
    case TYPE_MICOUT_U_READY:
    case TYPE_MICOUT_U_START:
    case TYPE_MICOUT_U_STOP:
    case TYPE_CAMFIX_U_READY:
    case TYPE_CAMFIX_U_START:
    case TYPE_CAMFIX_U_STOP:
    case TYPE_MICFIX_U_READY:
    case TYPE_MICFIX_U_START:
    case TYPE_MICFIX_U_STOP:
    case TYPE_INPUT_TOUCH_SINGLE:
    case TYPE_INPUT_KEY_SINGLE:
    case TYPE_INPUT_TEXT_SINGLE:
    case TYPE_MCTL_REBOOT:
    case TYPE_SYSTEM_REBOOT:
        sendData(data, size);
        break;
    }
}

void UserSession::connThread()
{
    char tempBuf[4096];
    while (!is_stop) {
        if (!is_connect) {
            mSocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
            struct sockaddr_in servaddr{};
            memset(&servaddr, 0, sizeof(servaddr));
            servaddr.sin_family = AF_INET;
            servaddr.sin_port = htons(REMOTEPC_SERVER_TCP_PORT);
            servaddr.sin_addr.s_addr = inet_addr(mSvIP);

            int ret = connect(mSocket, (struct sockaddr *) &servaddr, sizeof(servaddr));
            if (ret != 0) {
                FLOGD("UserSession connect failed! %s errno :%d", strerror(errno), errno);
                if (mSocket > 0) {
                    shutdown(mSocket, SHUT_RDWR);
                    close(mSocket);
                }
                for (int i = 0; i < 300; i++) {
                    usleep(10000);
                    if (is_stop) break;
                }
                continue;
            }

            FD_ZERO(&set);
            FD_SET(mSocket, &set);
            connected();

            //send U_LOGIN
            char data[sizeof(U_LOGIN)] = { 0 };
            memcpy(data, U_LOGIN, sizeof(U_LOGIN));
            memcpy(data+8, &mUid, 8);
            sendData(data, sizeof(U_LOGIN));
        }
        else {
            //tv.tv_sec = 2;
            //tv.tv_usec = 0;
            //int32_t ret = select(mSocket + 1, &set, nullptr, nullptr, &tv);
            //if (ret == 0) {
            //    FLOGD("UserSession::recvThread select read timeout, ret=[%d].", ret);
            //    continue;
            //}
            //if (FD_ISSET(mSocket, &set)) {
            int recvLen = recv(mSocket, tempBuf, 4096, 0);
            //FLOGD("UserSession recv data size[%d], errno=%d.", recvLen, errno);
            if (recvLen > 0) {
                std::lock_guard<std::mutex> lock_recv(mlock_recv);
                recvBuf.insert(recvBuf.end(), tempBuf, tempBuf + recvLen);
                mcond_recv.notify_one();
            }
            else {
                //TODO::disconnect 
                if (recvLen < 0 && (errno == EINTR || errno == EWOULDBLOCK || errno == EAGAIN)) {
                    continue;
                }    
                FLOGD("UserSession->recv len[%d], errno[%d]", recvLen, errno);
                disconnected();
                continue;                
            }
            //}
        }
    }
}

void UserSession::sendThread()
{
    std::vector<char> sendData;
    while (!is_stop) {
        {
            std::unique_lock<std::mutex> lock_conn(mlock_conn);
            while (!is_stop && !is_connect) {
                mcond_conn.wait(lock_conn);
            }
            if (is_stop) break;            
        }
        {
            std::unique_lock<std::mutex> lock_send(mlock_send);
            while (!is_stop && sendBuf.empty()) {
                mcond_send.wait(lock_send);
            }
            if (is_stop) break;
            if (sendBuf.size() > 0) {
                sendData.insert(sendData.end(), sendBuf.begin(), sendBuf.begin() + sendBuf.size());
                sendBuf.clear();
            }
        }
        while (!is_stop && sendData.size() > 0) {
            //FD_ZERO(&set);
            //FD_SET(mSocket, &set);
            //tv.tv_sec = 5;
            //tv.tv_usec = 0;
            //int32_t ret = select(mSocket + 1, nullptr, &set, nullptr, &tv);
            //if (ret <= 0) {
            //    FLOGE("UserClient sendThread select write timeout, ret=[%d].", ret);
            //    FD_CLR(mSocket, &set);
            //    disconnected();
            //    break;
            //}
            //if (FD_ISSET(mSocket, &set)) {
            int32_t sendLen = send(mSocket, &sendData[0], sendData.size(), 0);
            if (sendLen <= 0) {
                if (sendLen < 0 && (errno == EINTR || errno == EWOULDBLOCK || errno == EAGAIN)) {
                    //FD_CLR(mSocket, &set);
                    continue;
                }
                FLOGE("UserClient disconnect, socket[%d]sendLen[%d][%s(%d)].", mSocket, sendLen, strerror(errno), errno);
                //FD_CLR(mSocket, &set);
                disconnected();
                break;
            }
            sendData.erase(sendData.begin(), sendData.begin() + sendLen);
            //}
            //FD_CLR(mSocket, &set);
        }
    }
}

void UserSession::handleData()
{
    std::vector<char> handBuf;
    while (!is_stop) {
        {
            std::unique_lock<std::mutex> lock_recv(mlock_recv);
            while (!is_stop && recvBuf.size() < 8) {
                mcond_recv.wait(lock_recv);
            }
            if (is_stop) break;
            if (((recvBuf[0] & 0xFF) != 0xEE) || ((recvBuf[1] & 0xFF) != 0xAA)) {
                FLOGE("UserSession handleData bad header[%02x:%02x][%zu]", recvBuf[0] & 0xFF, recvBuf[1] & 0xFF, recvBuf.size());
                recvBuf.clear();
                continue;
            }
            int32_t dLen = (recvBuf[4] & 0xFF) << 24 | (recvBuf[5] & 0xFF) << 16 | (recvBuf[6] & 0xFF) << 8 | (recvBuf[7] & 0xFF);
            int32_t aLen = dLen + 8;
            while (!is_stop && (aLen > recvBuf.size())) {
                mcond_recv.wait(lock_recv);
            }
            if (is_stop) break;
            handBuf.clear();
            handBuf.insert(handBuf.end(), recvBuf.begin(), recvBuf.begin() + aLen);
            recvBuf.erase(recvBuf.begin(), recvBuf.begin() + aLen);
        }
        N->notifydata(&handBuf[0], handBuf.size());
    }
}

void UserSession::sendData(const char* data, int32_t size)
{
    if (!is_connect) return;
    std::lock_guard<std::mutex> lock_send(mlock_send);
    if (sendBuf.size() > TERMINAL_MAX_BUFFER) {
        FLOGD("NOTE::RtspClient send buffer too max, wile clean %zu size", sendBuf.size());
        sendBuf.clear();
    }
    sendBuf.insert(sendBuf.end(), data, data + size);
    mcond_send.notify_one();
}

void UserSession::connected()
{
    {
        std::lock_guard<std::mutex> lock_send(mlock_send);
        sendBuf.clear();
    }
    {
        std::lock_guard<std::mutex> lock_recv(mlock_recv);
        recvBuf.clear();
    }
    {
        std::unique_lock<std::mutex> lock_conn(mlock_conn);
        if (is_connect) return;
        is_connect = true;
        mcond_conn.notify_one();
    }
    FLOGD("UserSession connected.");
    N->miniNotify((const char*)U_CONNECTED, sizeof(U_CONNECTED), 0);
}

void UserSession::disconnected()
{
    {
        std::lock_guard<std::mutex> lock_conn(mlock_conn);
        if (!is_connect) return;
        is_connect = false;
    }
    {
        std::lock_guard<std::mutex> lock_send(mlock_send);
        sendBuf.clear();
    }
    {
        std::lock_guard<std::mutex> lock_recv(mlock_recv);
        recvBuf.clear();
    }

   if (mSocket > 0) {
        shutdown(mSocket, SHUT_RDWR);
        close(mSocket);
    }
    FLOGD("UserSession disconnected.");
    N->miniNotify((const char*)U_DISCONNECTED, sizeof(U_DISCONNECTED), 0);    
}

void UserSession::selfFixedThread()
{
    while (!is_stop) {
        for (int i = 0; i < 10; i++) {
            usleep(100000);
            if (is_stop) return;
        }

        int64_t currentTime = TimeUtil::uptimeUsec();

        //send heartbeat
        char us_heartbeat[sizeof(US_HEARTBEAT)];
        memcpy(us_heartbeat, US_HEARTBEAT, 8);
        memcpy(us_heartbeat + 8, &currentTime, 8);
        sendData(us_heartbeat, sizeof(US_HEARTBEAT));
    }
}