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
    void handle(NofifyType type, const char* data, int32_t size, const char* params) override;
    void disconnectClient(RtspClient *client);
    std::vector<char> get_sps(int channel);
    std::vector<char> get_pps(int channel);

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

    std::vector<char> vec_sps0;
    std::vector<char> vec_pps0;
    std::vector<char> vec_sps1;
    std::vector<char> vec_pps1;
    std::vector<char> vec_sps2;
    std::vector<char> vec_pps2;
    std::vector<char> vec_sps3;
    std::vector<char> vec_pps3;
};

#endif //F_ZEBRA_RTSPSERVER_H

