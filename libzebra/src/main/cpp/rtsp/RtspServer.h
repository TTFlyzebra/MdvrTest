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

    int32_t get_sps(int channel, char *sps, int32_t maxLen);

    int32_t get_pps(int channel, char *pps, int32_t maxLen);

    int32_t get_aacHead(int channel, char *aacHead, int32_t maxLen);

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

    char spss[256 * 4];
    int32_t spsLens[MAX_CAM];
    char ppss[128 * 4];
    int32_t ppsLens[MAX_CAM];
    char aacHeads[64 * 4];
    int32_t aacHeadLens[MAX_CAM];
};

#endif //F_ZEBRA_RTSPSERVER_H

