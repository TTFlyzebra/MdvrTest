//
// Created by FlyZebra on 2021/10/02 0016.
//

#ifndef F_ZEBRA_RRSPCLIENT_H
#define F_ZEBRA_RRSPCLIENT_H

#include <arpa/inet.h>
#include "base/BaseNotify.h"

class RtspServer;

class RtspClient:public BaseNotify {
public:
    RtspClient(RtspServer* server, Notify* notify, int32_t socket);
    ~RtspClient();
    void handle(NofifyType type, const char* data, int32_t size, const char* params) override;

private:
    void recvThread();
    void sendThread();
    void handleData();
    void videoRtpThread();
    void videoRtcpThread();
    void audioRtpThread();
    void audioRtcpThread();
    void selfFixedThread();
    void sendData(const char* data, int32_t size);
    void disconnect();

    void appendCommonResponse(std::string *response, int32_t cseq);
        
    void onOptionsRequest(const char* data, int32_t cseq);
    void onDescribeRequest(const char* data, int32_t cseq);
    void onSetupRequest(const char* data, int32_t cseq);
    void onPlayRequest(const char* data, int32_t cseq);
    void onGetParameterRequest(const char* data, int32_t cseq);
    void onOtherRequest(const char* data, int32_t cseq);

    void sendVFrame(const char* data, int32_t size, int64_t ptsUsec);
    void sendAFrame(const char* data, int32_t size, int64_t ptsUsec);

    int32_t createUdpSocket(int32_t *port);

private:
    RtspServer* mServer;
    int32_t mChannel;
    int32_t mSocket;
    bool is_disconnect;

    std::thread *send_t;
    std::mutex mlock_send;
    std::vector<char> sendBuf;
    std::condition_variable mcond_send;

    std::thread *recv_t;
    std::thread *hand_t;
    std::mutex mlock_recv;
    std::vector<char> recvBuf;
    std::condition_variable mcond_recv;

    int32_t is_use_tcp;

    int32_t v_rtp_socket;
    int32_t v_rtp_addrLen;
    struct sockaddr_in v_rtp_addr_in;
    std::thread *vrtp_t;

    int32_t v_rtcp_socket;
    int32_t v_rtcp_addrLen;
    struct sockaddr_in v_rtcp_addr_in;
    std::thread *vrtcp_t;

    int32_t a_rtp_socket;
    int32_t a_rtp_addrLen;
    struct sockaddr_in a_rtp_addr_in;
    std::thread *artp_t;

    int32_t a_rtcp_socket;
    int32_t a_rtcp_addrLen;
    struct sockaddr_in a_rtcp_addr_in;
    std::thread *artcp_t;

    int32_t sequencenumber1;
    int32_t sequencenumber2;

    std::thread *fixed_t;

    fd_set set;
    struct timeval tv;

    bool is_send_audiohead;
    bool is_hevc;
};

#endif //F_ZEBRA_RRSPCLIENT_H

