//
// Created by FlyZebra on 2020/10/22 0022.
//

#ifndef F_ZEBRA_RTSPSERVER_H
#define F_ZEBRA_RTSPSERVER_H

#include "base/BaseNotify.h"
#include "RtspClient.h"
#include "Config.h"
#include <map>

class RtspServer : public BaseNotify {
public:
    RtspServer(Notify *notify);

    ~RtspServer();

    void handle(NofifyType type, const char *data, int32_t size, const char *params) override;

    void disconnectClient(RtspClient *client);

    int32_t getVideoHead(int channel, char *videoHead);

    int32_t getAudioHead(int channel, char *audioHead);

private:
    void serverSocket();

    void removeClient();

private:
    int32_t server_socket;
    std::thread *server_t;
    std::mutex mlock_client;
    std::list<RtspClient *> rtsp_clients;
    std::thread *remove_t;
    std::mutex mlock_remove;
    std::vector<RtspClient *> remove_clients;
    std::condition_variable mcond_remove;

    char videoHeads[VIDEO_HEAD_MAX_SIZE * MAX_CAM];
    int32_t videoHeadLens[MAX_CAM];
    char audioHeads[AUDIO_HEAD_MAX_SIZE * MAX_CAM];
    int32_t audioHeadLens[MAX_CAM];
};

#endif //F_ZEBRA_RTSPSERVER_H

