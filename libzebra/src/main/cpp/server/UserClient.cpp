//
// Created by FlyZebra on 2021/9/16 0016.
//
#include "UserClient.h"

#include <fcntl.h>
#include <algorithm>
#include "UserServer.h"
#include "base/User.h"
#include "base/Terminal.h"
#include "Config.h"
#include "base/Global.h"
#include "utils/FlyLog.h"
#include "utils/ByteUtil.h"
#include "utils/TimeUtil.h"
#include "rfc/Protocol.h"
#include "Fzebra.h"

#include "nlohmann/json.hpp"
using nlohmann::json;

UserClient::UserClient(UserServer* server, Notify* notify, int32_t socket)
    : BaseNotify(notify)
    , mServer(server)
    , mSocket(socket)
    , recv_t(nullptr)
    , send_t(nullptr)
    , hand_t(nullptr)
    , fixed_t(nullptr)
{
    {
        N->registerListener(this);
        std::lock_guard<std::mutex> lock_stop(mlock_stop);
        is_stop = false;
    }
    lastHeartBeat = TimeUtil::uptimeUsec();
    recv_t = new std::thread(&UserClient::recvThread, this);
}

UserClient::~UserClient()
{
    {
        N->unregisterListener(this);
        std::lock_guard<std::mutex> lock_stop(mlock_stop);
        is_stop = true;
    }

    shutdown(mSocket, SD_BOTH);
#if defined(WIN32)
    closesocket(mSocket);
#elif defined(__unix)
    close(mSocket);
#endif
    {
        std::lock_guard<std::mutex> lock_send(mlock_send);
        mcond_send.notify_all();
    }
    {
        std::lock_guard<std::mutex> lock_recv(mlock_recv);
        mcond_recv.notify_all();
    }

    if (recv_t) {
        recv_t->join();
        delete recv_t;
    }
    if (send_t) {
        send_t->join();
        delete send_t;
    }
    if (hand_t) {
        hand_t->join();
        delete hand_t;
    }
    if (fixed_t) {
        fixed_t->join();
        delete fixed_t;
    }

    char suid[16] = { 0x00 };
    ByteUtil::int64ToSysId(suid, U.uid);
    FLOGD("[%s]%s()", suid, __func__);
}

void UserClient::notify(const char* data, int32_t size)
{
    std::lock_guard<std::mutex> lock_stop(mlock_stop);
    if (is_stop) return;
    auto* notifyData = (NotifyData*)data;
    //{
    //    std::lock_guard<std::mutex> lock_terminal(mlock_terminal);
    //    if (terminal_set.find(notifyData->tid) == terminal_set.end()) return;
    //}
    switch (notifyData->type) {
    case TYPE_TU_HEARTBEAT:
    case TYPE_T_CONNECTED:
    case TYPE_T_DISCONNECTED:
    case TYPE_T_INFO:
    case TYPE_SCREEN_T_START:
    case TYPE_SCREEN_T_STOP:
    case TYPE_SNDOUT_T_START:
    case TYPE_SNDOUT_T_STOP:
    case TYPE_CAMOUT_T_START:
    case TYPE_CAMOUT_T_STOP:
    case TYPE_MICOUT_T_START:
    case TYPE_MICOUT_T_STOP:
    case TYPE_CAMFIX_T_START:
    case TYPE_CAMFIX_T_STOP:
    case TYPE_MICFIX_T_START:
    case TYPE_MICFIX_T_STOP:
    case TYPE_CAMERA_OPEN:
    case TYPE_CAMERA_CLOSE:
    case TYPE_INPUT_MULTI_S_READY:
    case TYPE_INPUT_MULTI_S_STOP:
        sendData(data, size);
        break;
    case TYPE_SCREEN_AVC:
    {
        std::lock_guard<std::mutex> lock_screen(mlock_screen);
        if (screen_set.find(notifyData->tid) != screen_set.end()) sendData(data, size);
        break;
    }
    case TYPE_SNDOUT_AAC:
    {
        std::lock_guard<std::mutex> lock_sndout(mlock_sndout);
        if (sndout_set.find(notifyData->tid) != sndout_set.end()) sendData(data, size);
        break;
    }
    case TYPE_CAMOUT_AVC:
    {
        std::lock_guard<std::mutex> lock_camout(mlock_camout);
        if (camout_set.find(notifyData->tid) != camout_set.end()) sendData(data, size);
        break;
    }
    case TYPE_MICOUT_AAC:
    {
        std::lock_guard<std::mutex> lock_micout(mlock_micout);
        if (micout_set.find(notifyData->tid) != micout_set.end()) sendData(data, size);
        break;
    }
    }
}

void UserClient::handle(NofifyType type, const char* data, int32_t size, const char* params)
{
    switch (type) {
        case NOTI_SCREEN_SPS: {
            {
                std::lock_guard<std::mutex> lock_screen(mlock_screen);
                if (screen_set.find(T->tid) == screen_set.end()) return;
            }
            char screen_avc[sizeof(SCREEN_AVC)];
            memcpy(screen_avc, SCREEN_AVC, sizeof(SCREEN_AVC));
            int32_t dLen = size + sizeof(SCREEN_AVC) - 8;
            ByteUtil::int32ToData(screen_avc + 4, dLen);
            memcpy(screen_avc + 8, &T->tid, 8);
            std::lock_guard<std::mutex> lock(mlock_send);
            if (sendBuf.size() > MAX_SOCKET_BUFFER) {
                FLOGE("UserClient send buffer too max, wile clean %zu size", sendBuf.size());
                sendBuf.clear();
            }
            sendBuf.insert(sendBuf.end(), screen_avc, screen_avc+sizeof(SCREEN_AVC));
            sendBuf.insert(sendBuf.end(), data, data + size);
            mcond_send.notify_one();
            break;
        }
        case NOTI_SCREEN_AVC: {
            {
                std::lock_guard<std::mutex> lock_screen(mlock_screen);
                if (screen_set.find(T->tid) == screen_set.end()) return;
            }
            char screen_avc[sizeof(SCREEN_AVC)];
            memcpy(screen_avc, SCREEN_AVC, sizeof(SCREEN_AVC));
            int32_t dLen = size + sizeof(SCREEN_AVC) - 8;
            ByteUtil::int32ToData(screen_avc + 4, dLen);
            memcpy(screen_avc + 8, &T->tid, 8);
            std::lock_guard<std::mutex> lock(mlock_send);
            if (sendBuf.size() > MAX_SOCKET_BUFFER) {
                FLOGE("UserClient send buffer too max, wile clean %zu size", sendBuf.size());
                sendBuf.clear();
            }
            sendBuf.insert(sendBuf.end(), screen_avc, screen_avc+sizeof(SCREEN_AVC));
            sendBuf.insert(sendBuf.end(), data, data + size);
            mcond_send.notify_one();
            break;
        }
    }
}

int64_t UserClient::getUid()
{
    return U.uid;
}

void UserClient::recvThread()
{
    if (connected()) {
        send_t = new std::thread(&UserClient::sendThread, this);
        hand_t = new std::thread(&UserClient::handleData, this);
        fixed_t = new std::thread(&UserClient::selfFixedThread, this);

        char tempBuf[4096];
        while (!is_stop) {
            int recvLen = recv(mSocket, tempBuf, 4096, 0);
            //FLOGD("UserClient->recv len[%d], errno[%d]", recvLen, errno);
            if (recvLen > 0) {
                std::lock_guard<std::mutex> lock_recv(mlock_recv);
                recvBuf.insert(recvBuf.end(), tempBuf, tempBuf + recvLen);
                mcond_recv.notify_one();
            }
            else {
                if (recvLen < 0 && (errno == EINTR || errno == EWOULDBLOCK || errno == EAGAIN)) {
                    continue;
                }
                FLOGD("UserClient exit. recvLen[%d], errno[%s(%d)].", recvLen, strerror(errno), errno);
                break;
            }
        }
    }
    disconnected();
}

void UserClient::sendThread()
{
    std::vector<char> sendData;
    while (!is_stop) {
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
        int32_t sendLen = 0;
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
            sendLen = send(mSocket, &sendData[0], sendData.size(), 0);
            if (sendLen <= 0) {
                if (sendLen < 0 && (errno == EINTR || errno == EWOULDBLOCK || errno == EAGAIN)) {
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

void UserClient::handleData()
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
                FLOGE("UserClient handleData bad header[%02x:%02x]", recvBuf[0] & 0xFF, recvBuf[1] & 0xFF);
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

        const char* data = &handBuf[0];
        const NotifyData* notifyData = (NotifyData*)data;
        const int32_t size = handBuf.size();
        if (notifyData->type == TYPE_US_HEARTBEAT) {
            lastHeartBeat = TimeUtil::uptimeUsec();
            char su_heartbeat[sizeof(SU_HEARTBEAT)];
            memcpy(su_heartbeat, SU_HEARTBEAT, 8);
            memcpy(su_heartbeat + 8, data + 8, 8);
            sendData(su_heartbeat, sizeof(SU_HEARTBEAT));
            continue;
        }
        if(notifyData->type == TYPE_UT_HEARTBEAT){
            lastHeartBeat = TimeUtil::uptimeUsec();
            char ut_heartbeat[sizeof(TU_HEARTBEAT)];
            memcpy(ut_heartbeat, TU_HEARTBEAT, 8);
            memcpy(ut_heartbeat + 8, data + 8, 24);
            sendData(ut_heartbeat, sizeof(TU_HEARTBEAT));
        }
        switch (notifyData->type) {
        case TYPE_SCREEN_U_START:
        {
            std::lock_guard<std::mutex> lock_screen(mlock_screen);
            screen_set.emplace(notifyData->tid);
            break;
        }
        case TYPE_SCREEN_U_STOP:
        {
            std::lock_guard<std::mutex> lock_screen(mlock_screen);
            screen_set.erase(notifyData->tid);
            break;
        }
        case TYPE_SNDOUT_U_START:
        {
            std::lock_guard<std::mutex> lock_sndout(mlock_sndout);
            sndout_set.emplace(notifyData->tid);
            break;
        }
        case TYPE_SNDOUT_U_STOP:
        {
            std::lock_guard<std::mutex> lock_sndout(mlock_sndout);
            sndout_set.erase(notifyData->tid);
            break;
        }
        case TYPE_CAMOUT_U_READY:
        {
            std::lock_guard<std::mutex> lock_camout(mlock_camout);
            camout_set.emplace(notifyData->tid);
            break;
        }
        case TYPE_CAMOUT_U_STOP:
        {
            std::lock_guard<std::mutex> lock_camout(mlock_camout);
            camout_set.erase(notifyData->tid);
            break;
        }
        case TYPE_MICOUT_U_READY:
        {
            std::lock_guard<std::mutex> lock_micout(mlock_micout);
            micout_set.emplace(notifyData->tid);
            break;
        }
        case TYPE_MICOUT_U_STOP:
        {
            std::lock_guard<std::mutex> lock_micout(mlock_micout);
            micout_set.erase(notifyData->tid);
            break;
        }
        case TYPE_USER_TID_ADD:
        {
            std::lock_guard<std::mutex> lock_terminal(mlock_terminal);
            terminal_set.emplace(notifyData->tid);
            break;
        }
        case TYPE_USER_TID_REMOVE:
        {
            std::lock_guard<std::mutex> lock_terminal(mlock_terminal);
            terminal_set.erase(notifyData->tid);
            break;
        }
        }
        N->notifydata(data, size);
    }
}

void UserClient::sendData(const char* data, int32_t size)
{
    std::lock_guard<std::mutex> lock(mlock_send);
    if (sendBuf.size() > MAX_SOCKET_BUFFER) {
        FLOGE("UserClient send buffer too max, wile clean %zu size", sendBuf.size());
        sendBuf.clear();
    }
    sendBuf.insert(sendBuf.end(), data, data + size);
    mcond_send.notify_one();
}

bool UserClient::connected()
{
    char infoBuf[4096];
    int32_t recvLen = recv(mSocket, infoBuf, 8, 0);
    if (recvLen != 8) {
        FLOGE("user login checked error 1!");
        return false;
    }
    int32_t dLen = ByteUtil::getInt32(infoBuf + 4);
    recvLen = recv(mSocket, infoBuf + 8, dLen, 0);
    if (recvLen != dLen) {
        FLOGE("user login checked error 2!");
        return false;
    }

    NotifyData* notifyData = (NotifyData*)infoBuf;
    if (notifyData->type != TYPE_U_LOGIN) {
        FLOGE("user login checked error 3!");
        return false;
    }

     memcpy(&U.uid, infoBuf + 8, 8);

    struct sockaddr_in sockAddr;
#if defined(WIN32)
    int32_t sockLen = sizeof(sockAddr);
#elif defined(__unix)
    uint32_t sockLen = sizeof(sockAddr);
#endif
    getsockname(mSocket, (struct sockaddr*)&sockAddr, &sockLen);
    sprintf(U.sockaddr, "%s", inet_ntoa(sockAddr.sin_addr));
    U.sockport = ntohs(sockAddr.sin_port);
    struct sockaddr_in peerAddr;
#if defined(WIN32)
    int32_t peerLen = sizeof(sockAddr);
#elif defined(__unix)
    uint32_t peerLen = sizeof(sockAddr);
#endif
    getpeername(mSocket, (struct sockaddr*)&peerAddr, &peerLen);
    sprintf(U.peeraddr, "%s", inet_ntoa(peerAddr.sin_addr));
    U.peerport = ntohs(peerAddr.sin_port);

    char sn[16] = { 0 };
    ByteUtil::int64ToSysId(sn, U.uid);
    FLOGD("[%s][U]%s:%d-->%s:%d", sn, U.peeraddr, U.peerport, U.sockaddr, U.sockport);
    N->miniNotify((const char*)T_CONNECTED, sizeof(T_CONNECTED), T->tid);
    N->miniNotify((const char*)U_CONNECTED, sizeof(U_CONNECTED), T->tid, U.uid);
    return true;
}

void UserClient::disconnected()
{
    {
        std::lock_guard<std::mutex> lock_stop(mlock_stop);
        if (is_stop) return;
        is_stop = true;
    }
    mServer->disconnectClient(this);
    N->miniNotify((const char*)T_DISCONNECTED, sizeof(T_DISCONNECTED), T->tid);
    N->miniNotify((const char*)U_DISCONNECTED, sizeof(U_DISCONNECTED), T->tid, U.uid);
}

void UserClient::selfFixedThread()
{
    while (!is_stop) {
        for (int i = 0; i < 10; i++) {
#if defined(WIN32)
            _sleep(100);
#elif defined(__unix)
            usleep(100000);
#endif
            if (is_stop) return;
        }

        int64_t currentTime = TimeUtil::uptimeUsec();
        char ts_heartbeat[sizeof(TS_HEARTBEAT)];
        memcpy(ts_heartbeat, TS_HEARTBEAT, 8);
        memcpy(ts_heartbeat + 8, &currentTime, 8);
        sendData(ts_heartbeat, sizeof(TS_HEARTBEAT));

        //check heartbeat.
        int64_t wait_time = TimeUtil::uptimeUsec() - lastHeartBeat;
        if (wait_time > US_WAITTIME && !is_stop) {
            FLOGE("check heartbeat time out, %ld, disconnected.", wait_time);
            disconnected();
            return;
        }
    }
}
