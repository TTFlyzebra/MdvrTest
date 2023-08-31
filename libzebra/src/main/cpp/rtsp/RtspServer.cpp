//
// Created by FlyZebra on 2020/10/22 0022.
//

#include "RtspServer.h"

#include <stdio.h>
#include <errno.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <unistd.h>
#include "utils/FlyLog.h"
#include "Config.h"
#include "rfc/Protocol.h"
#include "utils/SysUtil.h"
#include "utils/ByteUtil.h"

RtspServer::RtspServer(Notify *notify)
        : BaseNotify(notify), server_socket(-1) {
    FLOGD("%s()", __func__);
    server_t = new std::thread(&RtspServer::serverSocket, this);
    SysUtil::setThreadName(server_t, "RtspServer-server");
    remove_t = new std::thread(&RtspServer::removeClient, this);
    SysUtil::setThreadName(remove_t, "RtspServer-remove");
}

RtspServer::~RtspServer() {
    is_stop = true;
    if (server_socket > 0) {
        shutdown(server_socket, SHUT_RDWR);
        close(server_socket);
        server_socket = -1;
    }
    {
        std::lock_guard<std::mutex> lock(mlock_remove);
        mcond_remove.notify_all();
    }
    {
        std::lock_guard<std::mutex> lock(mlock_client);
        for (auto &rtsp_client: rtsp_clients) {
            delete ((RtspClient *) rtsp_client);
        }
        rtsp_clients.clear();
    }
    server_t->join();
    remove_t->join();
    delete server_t;
    delete remove_t;
    FLOGD("%s()", __func__);
}

void RtspServer::handle(NofifyType type, const char *data, int32_t size, const char *params) {
    if (type == NOTI_CAMOUT_SPS) {
        int16_t channel = ByteUtil::byte2int16(params, 0, true);
        videoHeadLens[channel] = size;
        memcpy(videoHeads + VIDEO_HEAD_MAX_SIZE * channel, data, size);
    } else if (type == NOTI_MICOUT_SPS) {
        int16_t channel = ByteUtil::byte2int16(params, 0, true);
        audioHeadLens[channel] = size;
        memcpy(audioHeads + AUDIO_HEAD_MAX_SIZE * channel, data, size);
    }
}

void RtspServer::serverSocket() {
    while (!is_stop) {
        server_socket = socket(AF_INET, SOCK_STREAM, 0);
        if (server_socket <= 0) {
            FLOGE("RtspServer serverSocket socket error. socket[%d][%s(%d)]", server_socket,
                  strerror(errno), errno);
            server_socket = -1;
            usleep(1000000);
            continue;
        }
        struct sockaddr_in t_sockaddr{};
        memset(&t_sockaddr, 0, sizeof(t_sockaddr));
        t_sockaddr.sin_family = AF_INET;
        t_sockaddr.sin_addr.s_addr = htonl(INADDR_ANY);
        t_sockaddr.sin_port = htons(RTSP_SERVER_TCP_PORT);
        int32_t opt = 1;
        int32_t ret = setsockopt(server_socket, SOL_SOCKET, SO_REUSEADDR, (const void *) &opt,
                                 sizeof(opt));
        if (ret < 0) {
            FLOGE("RtspServer setsockopt SO_REUSEADDR error.");
        }
        ret = bind(server_socket, (struct sockaddr *) &t_sockaddr, sizeof(t_sockaddr));
        if (ret < 0) {
            FLOGE("RtspServer serverSocket bind %d socket error. [%s(%d)]", RTSP_SERVER_TCP_PORT,
                  strerror(errno), errno);
            shutdown(server_socket, SHUT_RDWR);
            close(server_socket);
            server_socket = -1;
            for (int i = 0; i < 100; i++) {
                usleep(100000);
                if (is_stop) return;
            }
            continue;
        }
        ret = listen(server_socket, 5);
        if (ret < 0) {
            FLOGE("RtspServer serverSocket listen error. [%s(%d)]", strerror(errno), errno);
            shutdown(server_socket, SHUT_RDWR);
            close(server_socket);
            server_socket = -1;
            usleep(1000000);
            continue;
        }
        while (!is_stop) {
            int32_t client_socket = accept(server_socket, (struct sockaddr *) nullptr, nullptr);
            if (client_socket <= 0) {
                if (!is_stop) {
                    FLOGE("RtspServer accpet socket error. [%s(%d)]", strerror(errno), errno);
                    continue;
                } else {
                    break;
                }
            }
            if (is_stop) break;
            auto *client = new RtspClient(this, N, client_socket);
            std::lock_guard<std::mutex> lock(mlock_client);
            rtsp_clients.push_back(client);
        }
        shutdown(server_socket, SHUT_RDWR);
        close(server_socket);
        server_socket = -1;
    }
}

void RtspServer::removeClient() {
    while (!is_stop) {
        std::unique_lock<std::mutex> lock_remove(mlock_remove);
        while (!is_stop && remove_clients.empty()) {
            mcond_remove.wait(lock_remove);
        }
        if (is_stop) break;
        for (auto &remove_client: remove_clients) {
            {
                std::lock_guard<std::mutex> lock_client(mlock_client);
                rtsp_clients.remove(((RtspClient *) remove_client));
            }
            delete ((RtspClient *) remove_client);
        }
        remove_clients.clear();
        FLOGD("RtspServer::removeClient rtsp_clients.size=%zu", rtsp_clients.size());
    }
}

void RtspServer::disconnectClient(RtspClient *client) {
    std::lock_guard<std::mutex> lock(mlock_remove);
    remove_clients.push_back(client);
    mcond_remove.notify_one();
}

int32_t RtspServer::getVideoHead(int channel, char *videoHead) {
    if (channel >= MAX_CAM) return 0;
    memcpy(videoHead, videoHeads + VIDEO_HEAD_MAX_SIZE * channel, videoHeadLens[channel]);
    return videoHeadLens[channel];
}

int32_t RtspServer::getAudioHead(int channel, char *audioHead) {
    if (channel >= MAX_CAM) return 0;
    memcpy(audioHead, audioHeads + AUDIO_HEAD_MAX_SIZE * channel, audioHeadLens[channel]);
    return audioHeadLens[channel];
}
