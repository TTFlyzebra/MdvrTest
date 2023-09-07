//
// Created by FlyZebra on 2021/9/16 0016.
//
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

#include "UserServer.h"

#include "Config.h"
#include "utils/FlyLog.h"
#include "utils/ByteUtil.h"
#include "UserClient.h"

UserServer::UserServer(Notify* notify)
    : BaseNotify(notify)
{
    FLOGD("%s()", __func__);
    {
        std::lock_guard<std::mutex> lock_stop(mlock_stop);
        is_stop = false;
    }
    server_t = new std::thread(&UserServer::serverSocket, this);
    remove_t = new std::thread(&UserServer::removeClient, this);
}

UserServer::~UserServer()
{
    {
        std::lock_guard<std::mutex> lock_stop(mlock_stop);
        is_stop = true;
    }

    shutdown(server_socket, SD_BOTH);
#if defined(WIN32)
    closesocket(server_socket);
#elif defined(__unix)
    close(server_socket);
#endif
    {
        std::lock_guard<std::mutex> lock_server(mlock_users);
        for (auto & users_client : users_clients) {
            delete ((UserClient*)users_client);
        }
        users_clients.clear();
    }
    {
        std::lock_guard<std::mutex> lock_remove(mlock_remove);
        mcond_remove.notify_one();
    }
    server_t->join();
    remove_t->join();
    delete server_t;
    delete remove_t;
    FLOGD("%s()", __func__);
}

void UserServer::serverSocket()
{
    FLOGD("UserServer serverSocket start!");
    struct sockaddr_in t_sockaddr{};
    memset(&t_sockaddr, 0, sizeof(t_sockaddr));
    t_sockaddr.sin_family = AF_INET;
    t_sockaddr.sin_addr.s_addr = htonl(INADDR_ANY);
    t_sockaddr.sin_port = htons(REMOTEPC_SERVER_TCP_PORT);
    server_socket = socket(AF_INET, SOCK_STREAM, 0);
    if (server_socket < 0) {
        FLOGD("UserServer socket server error %s errno: %d", strerror(errno), errno);
        return;
    }
    int32_t flag = 1;
    if (setsockopt(server_socket, SOL_SOCKET, SO_REUSEADDR, (char*)&flag, sizeof(flag)) == -1) {
        FLOGE("setsockopt SO_REUSEADDR failed!");
    }
    int32_t ret = bind(server_socket, (struct sockaddr*)&t_sockaddr, sizeof(t_sockaddr));
    if (ret < 0) {
        FLOGD("UserServer bind %d socket error %s errno: %d", REMOTEPC_SERVER_TCP_PORT, strerror(errno), errno);
        return;
    }
    ret = listen(server_socket, 1024);
    if (ret < 0) {
        FLOGD("UserServer listen error %s errno: %d", strerror(errno), errno);
    }
    while (!is_stop) {
        int32_t client_socket = accept(server_socket, (struct sockaddr*)nullptr, nullptr);
        if (client_socket < 0) {
            FLOGD("UserServer accpet socket error: %s errno :%d", strerror(errno), errno);
            continue;
        }
        if (is_stop) break;
        auto* client = new UserClient(this, N, client_socket);
        std::lock_guard<std::mutex> lock_server(mlock_users);
        users_clients.push_back(client);
    }
    if (server_socket >= 0) {
#if defined(WIN32)
        closesocket(server_socket);
#elif defined(__unix)
        close(server_socket);
#endif
        server_socket = -1;
    }
    FLOGD("UserServer serverSocket exit!");
}

void UserServer::removeClient()
{
    while (!is_stop) {
        std::unique_lock<std::mutex> lock_remove(mlock_remove);
        while (!is_stop && remove_clients.empty()) {
            mcond_remove.wait(lock_remove);
        }
        if (is_stop) break;
        for (auto userClient : remove_clients) {
            char sn[16] = { 0 };
            ByteUtil::int64ToSysId(sn, userClient->getUid());
            FLOGD("[%s][U]->removed.", sn);
            delete userClient;
        }
        remove_clients.clear();
    }
}

void UserServer::disconnectClient(UserClient* client)
{
    {
        std::lock_guard<std::mutex> lock_terminals(mlock_users);
        users_clients.remove(client);
    }
    {
        std::lock_guard<std::mutex> lock_remove(mlock_remove);
        remove_clients.push_back(client);
        mcond_remove.notify_one();
    }
}

