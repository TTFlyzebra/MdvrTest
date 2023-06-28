//
// Created by FlyZebra on 2020/10/22 0022.
//

#ifndef F_ZEBRA_RTSPSERVER_H
#define F_ZEBRA_RTSPSERVER_H

#include "base/BaseNotify.h"
#include "RtspClient.h"

class RtspServer : public BaseNotify {
public:
    RtspServer(Notify* notify);
    ~RtspServer();
    void disconnectClient(RtspClient *client);

private:
    void serverSocket();
    void removeClient();

private:
    int32_t server_socket;
    std::thread *server_t;
    std::mutex mlock_client;
    std::list<RtspClient*> rtsp_clients;
    std::thread *remove_t;
    std::mutex mlock_remove;
    std::vector<RtspClient*> remove_clients;
    std::condition_variable mcond_remove;
};

#endif //F_ZEBRA_RTSPSERVER_H

