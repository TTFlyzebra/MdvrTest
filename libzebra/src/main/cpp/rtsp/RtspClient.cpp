//
// Created by FlyZebra on 2021/9/16 0016.
//

#include "RtspClient.h"

#include <stdio.h>
#include <errno.h>
#include <unistd.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <string>
#include <sys/syscall.h>
#include "Config.h"
#include "rfc/Protocol.h"
#include "utils/FlyLog.h"
#include "utils/Base64.h"
#include "utils/ByteUtil.h"
#include "RtspServer.h"
#include "utils/SysUtil.h"

RtspClient::RtspClient(RtspServer *server, Notify *notify, int32_t socket)
        : BaseNotify(notify), mServer(server), mChannel(0), mSocket(socket), is_disconnect(false),
          is_use_tcp(true), v_rtp_socket(-1), vrtp_t(nullptr), v_rtcp_socket(-1), vrtcp_t(nullptr),
          a_rtp_socket(-1), artp_t(nullptr), a_rtcp_socket(-1), artcp_t(nullptr),
          sequencenumber1(1234), sequencenumber2(4321), is_send_audiohead(false), is_hevc(false) {
    FLOGD("%s()", __func__);
    recv_t = new std::thread(&RtspClient::recvThread, this);
    SysUtil::setThreadName(recv_t, "RtspClient-recv");
    send_t = new std::thread(&RtspClient::sendThread, this);
    SysUtil::setThreadName(send_t, "RtspClient-send");
    hand_t = new std::thread(&RtspClient::handleData, this);
    SysUtil::setThreadName(hand_t, "RtspClient-hand");
    fixed_t = new std::thread(&RtspClient::selfFixedThread, this);
    SysUtil::setThreadName(fixed_t, "RtspClient-fixed");
}

RtspClient::~RtspClient() {
    is_stop = true;
    shutdown(mSocket, SHUT_RDWR);
    close(mSocket);
    {
        std::lock_guard<std::mutex> lock(mlock_send);
        mcond_send.notify_all();
    }
    {
        std::lock_guard<std::mutex> lock(mlock_recv);
        mcond_recv.notify_all();
    }

    if (v_rtp_socket > 0) {
        shutdown(v_rtp_socket, SHUT_RDWR);
        close(v_rtp_socket);
        v_rtp_socket = -1;
    }
    if (vrtp_t) {
        vrtp_t->join();
        delete vrtp_t;
    }
    if (v_rtcp_socket > 0) {
        shutdown(v_rtcp_socket, SHUT_RDWR);
        close(v_rtcp_socket);
        v_rtcp_socket = -1;
    }
    if (vrtcp_t) {
        vrtcp_t->join();
        delete vrtcp_t;
    }
    if (a_rtp_socket > 0) {
        shutdown(a_rtp_socket, SHUT_RDWR);
        close(a_rtp_socket);
        a_rtp_socket = -1;
    }
    if (artp_t) {
        artp_t->join();
        delete artp_t;
    }
    if (a_rtcp_socket > 0) {
        shutdown(a_rtcp_socket, SHUT_RDWR);
        close(a_rtcp_socket);
        a_rtcp_socket = -1;
    }
    if (artcp_t) {
        artcp_t->join();
        delete artcp_t;
    }
    recv_t->join();
    send_t->join();
    hand_t->join();
    fixed_t->join();
    delete recv_t;
    delete send_t;
    delete hand_t;
    delete fixed_t;
    FLOGD("%s()", __func__);
}

void RtspClient::handle(NofifyType type, const char *data, int32_t size, const char *params) {
    switch (type) {
        case NOTI_CAMOUT_AVC: {
            int16_t channel = ByteUtil::byte2int16(params, 0, true);
            if (mChannel == channel) {
                int64_t pts = ByteUtil::byte2int64(params, 2, true);
                sendVFrame(data, size, pts);
            }
            break;
        }
        case NOTI_MICOUT_AAC: {
            int16_t channel = ByteUtil::byte2int16(params, 0, true);
            if (mChannel == channel) {
                if (!is_send_audiohead) {
                    char audioHead[AUDIO_HEAD_MAX_SIZE];
                    int32_t audioHeadLen = mServer->getAudioHead(mChannel, audioHead);
                    sendAFrame(audioHead, audioHeadLen, 0);
                    is_send_audiohead = true;
                }
                int64_t pts = ByteUtil::byte2int64(params, 2, true);
                sendAFrame(data, size, pts);
            }
            break;
        }
    }
}

void RtspClient::recvThread() {
    char tempBuf[4096];
    while (!is_stop) {
        memset(tempBuf, 0, 4096);
        int32_t recvLen = recv(mSocket, tempBuf, 4096, 0);
        FLOGD("RtspClient recv:len[%d], errno[%d]\n%s", recvLen, errno, tempBuf);
        if (recvLen <= 0) {
            if (recvLen < 0 && errno == EAGAIN) {
                continue;
            }
            FLOGD("RtspClient disconnect, socket[%d]recvLen[%d][%s(%d)].", mSocket, recvLen,
                  strerror(errno), errno);
            is_stop = true;
            break;
        } else {
            std::lock_guard<std::mutex> lock(mlock_recv);
            recvBuf.insert(recvBuf.end(), tempBuf, tempBuf + recvLen);
            mcond_recv.notify_one();
        }
    }
    disconnect();
}

void RtspClient::sendThread() {
    std::vector<char> sendData;
    while (!is_stop) {
        {
            std::unique_lock<std::mutex> lock(mlock_send);
            while (!is_stop && sendBuf.empty()) {
                mcond_send.wait(lock);
            }
            if (is_stop) break;
            if (sendBuf.size() > 0) {
                sendData.insert(sendData.end(), sendBuf.begin(), sendBuf.begin() + sendBuf.size());
                sendBuf.clear();
            }
        }
        while (!is_stop && !sendData.empty()) {
            tv.tv_sec = 5;
            tv.tv_usec = 0;
            FD_ZERO(&set);
            FD_SET(mSocket, &set);
            int32_t ret = select(mSocket + 1, nullptr, &set, nullptr, &tv);
            if (ret == 0) {
                FLOGD("RtspClient sendThread select write error. socket[%d]ret[%d].",mSocket, ret);
                disconnect();
                break;
            }
            if (FD_ISSET(mSocket, &set)) {
                int32_t sendLen = send(mSocket, &sendData[0], sendData.size(), 0);
                if (sendLen > 0) {
                    sendData.erase(sendData.begin(), sendData.begin() + sendLen);
                } else {
                    if (sendLen < 0 && errno == EAGAIN) {
                        FD_CLR(mSocket, &set);
                        continue;
                    }
                    disconnect();
                }
            }
            FD_CLR(mSocket, &set);
        }
    }
}

void RtspClient::handleData() {
    while (!is_stop) {
        std::unique_lock<std::mutex> lock(mlock_recv);
        while (!is_stop && recvBuf.empty()) {
            mcond_recv.wait(lock);
        }
        if (is_stop) break;
        char url[512] = {0};
        char ver[64] = {0};
        char action[64] = {0};
        if (sscanf((const char *) &recvBuf[0], "%s %s %s\r", action, url, ver) == 3) {
            int32_t cseq;
            sscanf(strstr((const char *) &recvBuf[0], "CSeq"), "CSeq: %d", &cseq);
            std::string method(action);
            if (method == "OPTIONS") {
                onOptionsRequest((const char *) &recvBuf[0], cseq);
            } else if (method == "DESCRIBE") {
                onDescribeRequest((const char *) &recvBuf[0], cseq);
            } else if (method == "SETUP") {
                onSetupRequest((const char *) &recvBuf[0], cseq);
            } else if (method == "PLAY") {
                onPlayRequest((const char *) &recvBuf[0], cseq);
            } else if (method == "GET_PARAMETER") {
                onGetParameterRequest((const char *) &recvBuf[0], cseq);
            }
        } else {
            onOtherRequest((const char *) &recvBuf[0], -1);
        }
        recvBuf.clear();
    }
}


void RtspClient::videoRtpThread() {
    char recvdata[1024];
    char temp[4096];
    int32_t socket = v_rtp_socket;
    while (!is_stop) {
        int32_t recvLen = recvfrom(socket, recvdata, 1024, 0, (struct sockaddr *) &v_rtp_addr_in,
                                   (socklen_t *) &v_rtp_addrLen);
        if (recvLen > 0) {
            memset(temp, 0, 4096);
            for (int32_t i = 0; i < recvLen; i++) {
                sprintf(temp, "%s%02x:", temp, recvBuf[i]);
            }
            FLOGE("v_rtp recv:[%d][%d]\n%s", recvLen, errno, temp);
        } else {
            if (recvLen < 0 && errno == EAGAIN) {
                continue;
            }
            disconnect();
            FLOGD("RtspClient Video rtp recv exit! socket[%d]recvLen[%d][%s(%d)].", socket, recvLen,
                  strerror(errno), errno);
            break;
        }
    }
}

void RtspClient::videoRtcpThread() {
    char recvdata[1024];
    char temp[4096];
    int32_t socket = v_rtcp_socket;
    while (!is_stop) {
        int32_t recvLen = recvfrom(socket, recvdata, 1024, 0, (struct sockaddr *) &v_rtcp_addr_in,
                                   (socklen_t *) &v_rtcp_addrLen);
        if (recvLen > 0) {
            memset(temp, 0, 4096);
            for (int32_t i = 0; i < recvLen; i++) {
                sprintf(temp, "%s%02x:", temp, recvBuf[i]);
            }
            FLOGE("v_rtcp recv:[%d][%d]\n%s", recvLen, errno, temp);
        } else {
            if (recvLen < 0 && errno == EAGAIN) {
                continue;
            }
            FLOGD("RtspClient Video rtcp recv exit! socket[%d]recvLen[%d][%s(%d)].", socket,
                  recvLen, strerror(errno), errno);
            disconnect();
            break;
        }
    }
}

void RtspClient::audioRtpThread() {
    char recvdata[1024];
    char temp[4096];
    int32_t socket = a_rtp_socket;
    while (!is_stop) {
        int32_t recvLen = recvfrom(socket, recvdata, 1024, 0, (struct sockaddr *) &a_rtp_addr_in,
                                   (socklen_t *) &a_rtp_addrLen);
        if (recvLen > 0) {
            memset(temp, 0, 4096);
            for (int32_t i = 0; i < recvLen; i++) {
                sprintf(temp, "%s%02x:", temp, recvBuf[i]);
            }
            FLOGE("a_rtp recv:[%d][%d]\n%s", recvLen, errno, temp);
        } else {
            if (recvLen < 0 && errno == EAGAIN) {
                continue;
            }
            FLOGD("RtspClient Audio rtp recv exit! socket[%d]recvLen[%d][%s(%d)].", socket, recvLen,
                  strerror(errno), errno);
            disconnect();
            break;
        }
    }
}

void RtspClient::audioRtcpThread() {
    char recvdata[1024];
    char temp[4096];
    int32_t socket = a_rtcp_socket;
    while (!is_stop) {
        int32_t recvLen = recvfrom(socket, recvdata, 1024, 0, (struct sockaddr *) &a_rtcp_addr_in,
                                   (socklen_t *) &a_rtcp_addrLen);
        if (recvLen > 0) {
            memset(temp, 0, 4096);
            for (int32_t i = 0; i < recvLen; i++) {
                sprintf(temp, "%s%02x:", temp, recvBuf[i]);
            }
            FLOGE("a_rtcp recv:[%d][%d]\n%s", recvLen, errno, temp);
        } else {
            if (recvLen < 0 && errno == EAGAIN) {
                continue;
            }
            FLOGD("RtspClient Audio rtcp recv exit! socket[%d]recvLen[%d][%s(%d)].", socket,
                  recvLen, strerror(errno), errno);
            disconnect();
            break;
        }
    }
}

void RtspClient::sendData(const char *data, int32_t size) {
    std::lock_guard<std::mutex> lock(mlock_send);
    if (sendBuf.size() > TERMINAL_MAX_BUFFER) {
        FLOGD("NOTE::RtspClient send buffer too max, will clean %zu size", sendBuf.size());
        //TODO::reset avc recrod for I frame
        //sendBuf.clear();
        disconnect();
        return;
    }
    sendBuf.insert(sendBuf.end(), data, data + size);
    mcond_send.notify_one();
}


void RtspClient::disconnect() {
    if (!is_disconnect) {
        is_disconnect = true;
        shutdown(mSocket, SHUT_RDWR);
        close(mSocket);
        mServer->disconnectClient(this);
    }
}


void RtspClient::appendCommonResponse(std::string *response, int32_t cseq) {
    char temp[128] = {0};
    sprintf(temp, "CSeq: %d\r\n", cseq);
    response->append(temp);
    response->append("User-Agent: Android rtsp server(Author zebra)\r\n");
    time_t now1 = time(nullptr);
    struct tm *now2 = gmtime(&now1);
    memset(temp, 0, strlen(temp));
    strftime(temp, sizeof(temp), "%a, %d %b %Y %H:%M:%S %z", now2);
    response->append("Date: ");
    response->append(temp);
    response->append("\r\n");
}


void RtspClient::onOptionsRequest(const char *data, int32_t cseq) {
    if (strstr((const char *) data, ":8554/camera1") != nullptr) {
        mChannel = 0;
    } else if (strstr((const char *) data, ":8554/camera2") != nullptr) {
        mChannel = 1;
    } else if (strstr((const char *) data, ":8554/camera3") != nullptr) {
        mChannel = 2;
    } else if (strstr((const char *) data, ":8554/camera4") != nullptr) {
        mChannel = 3;
    }
    std::string response = "RTSP/1.0 200 OK\r\n";
    appendCommonResponse(&response, cseq);
    response.append(
            "Public: OPTIONS, DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE, GET_PARAMETER, SET_PARAMETER\r\n");
    response.append("\r\n");
    int32_t ret = send(mSocket, response.c_str(), response.size(), 0);
    FLOGD("RtspClient send:len[%d], errno[%d]\n%s", ret, errno, response.c_str());
    if (ret <= 0) disconnect();
}

void RtspClient::onDescribeRequest(const char *data, int32_t cseq) {
    char temp[1024];
    struct sockaddr_in addr{};
    socklen_t addrlen = sizeof(addr);
    getsockname(mSocket, (struct sockaddr *) &addr, &addrlen);
    std::string response = "RTSP/1.0 200 OK\r\n";
    appendCommonResponse(&response, cseq);
    std::string sdp;
    sdp.append("v=0\r\n");
    sprintf(temp, "o=- 1627453750119587 1 in IP4 %s\r\n", inet_ntoa(addr.sin_addr));
    sdp.append(temp);
    sdp.append("t=0 0\r\n");
    sdp.append("a=contol:*\r\n");

    char videoHead[VIDEO_HEAD_MAX_SIZE] = {0};
    int32_t videoHeadLen = mServer->getVideoHead(mChannel, videoHead);

    int ptr_0 = -1;
    int ptr_1 = -1;
    int ptr_2 = -1;
    for (int i = 0; i < videoHeadLen - 4; i++) {
        if (videoHead[i] == 0x00 && videoHead[i + 1] == 0x00 &&
            videoHead[i + 2] == 0x00 && videoHead[i + 3] == 0x01) {
            if (ptr_0 == -1) {
                ptr_0 = i;
                i += 3;
            } else if (ptr_1 == -1) {
                ptr_1 = i;
                i += 3;
            } else {
                ptr_2 = i;
                break;
            }
        }
    }
    if (ptr_0 == -1 || ptr_1 == -1) {
        FLOGE("RtspServer Get sps pps error!");
        return;
    }

    is_hevc = (ptr_2 != -1);
    //avc
    if (!is_hevc) {
        int spsLen = ptr_1 - 4;
        const uint8_t *sps = reinterpret_cast<const uint8_t *>(videoHead + ptr_0 + 4);
        int ppsLen = videoHeadLen - ptr_1 - 4;
        const uint8_t *pps = reinterpret_cast<const uint8_t *>(videoHead + ptr_1 + 4);

        int outspsLen;
        int outppsLen;
        uint8_t outsps[1024]{0};
        uint8_t outpps[1024]{0};
        Base64::encode(sps, spsLen, outsps, &outspsLen);
        Base64::encode(pps, ppsLen, outpps, &outppsLen);

        memset(temp, 0, strlen(temp));
        sdp.append("m=video 0 RTP/AVP 96\r\n");
        sdp.append("a=rtpmap:96 H264/90000\r\n");
        sdp.append("a=fmtp:96 profile-level-id=42001f;packetization-mode=1;");
        sprintf(temp, "sprop-parameter-sets=%s,%s\r\n", outsps, outpps);
        sdp.append(temp);
    }
        //hevc
    else {
        int vpsLen = ptr_1 - 4;
        const uint8_t *vps = reinterpret_cast<const uint8_t *>(videoHead + ptr_0 + 4);
        int spsLen = ptr_2 - ptr_1 - 4;
        const uint8_t *sps = reinterpret_cast<const uint8_t *>(videoHead + ptr_1 + 4);
        int ppsLen = videoHeadLen - ptr_2 - 4;
        const uint8_t *pps = reinterpret_cast<const uint8_t *>(videoHead + ptr_2 + 4);

        int outvpsLen;
        int outspsLen;
        int outppsLen;
        uint8_t outvps[1024]{0};
        uint8_t outsps[1024]{0};
        uint8_t outpps[1024]{0};
        Base64::encode(vps, vpsLen, outvps, &outvpsLen);
        Base64::encode(sps, spsLen, outsps, &outspsLen);
        Base64::encode(pps, ppsLen, outpps, &outppsLen);

        memset(temp, 0, strlen(temp));
        sdp.append("m=video 0 RTP/AVP 96\r\n");
        sdp.append("a=rtpmap:96 H265/90000\r\n");
        sdp.append("a=fmtp:96 profile-level-id=42001f;");
        sprintf(temp, "sprop-vps=%s;sprop-sps=%s;sprop-pps=%s\r\n", outvps, outsps, outpps);
        sdp.append(temp);
    }

    sdp.append("a=control:track1\r\n");
    sdp.append("m=audio 0 RTP/AVP 97\r\n");
    switch (AUDIO_SAMPLE) {
        case 48000:
            sdp.append("a=rtpmap:97 mpeg4-generic/48000/2\r\n");
            sdp.append("a=fmtp:97 streamtype=5;profile-level-id=15;mode=AAC-hbr;");
            sdp.append("sizelength=13;indexlength=3;indexdeltalength=3;config=1190;Profile=1;\r\n");
            break;
        case 44100:
            sdp.append("a=rtpmap:97 mpeg4-generic/44100/2\r\n");
            sdp.append("a=fmtp:97 streamtype=5;profile-level-id=15;mode=AAC-hbr;");
            sdp.append("sizelength=13;indexlength=3;indexdeltalength=3;config=1210;Profile=1;\r\n");
            break;
        default:
            sdp.append("a=rtpmap:97 mpeg4-generic/16000/2\r\n");
            sdp.append("a=fmtp:97 streamtype=5;profile-level-id=15;mode=AAC-hbr;");
            sdp.append("sizelength=13;indexlength=3;indexdeltalength=3;config=1410;Profile=1;\r\n");
            break;
    }
    sdp.append("a=control:track2\r\n\r\n");
    memset(temp, 0, strlen(temp));
    sprintf(temp, "Content-Length: %d\r\n", (int32_t) sdp.size());
    response.append(temp);
    response.append("Content-Type: application/sdp\r\n");
    response.append("\r\n");
    response.append(sdp.c_str());
    int32_t ret = send(mSocket, response.c_str(), response.size(), 0);
    FLOGD("RtspClient send:len[%d], errno[%d]\n%s", ret, errno, response.c_str());
    if (ret <= 0) disconnect();
}

int32_t RtspClient::createUdpSocket(int32_t *port) {
    int32_t sock_fd = -1;
    struct sockaddr_in addr_in{};
    sock_fd = socket(AF_INET, SOCK_DGRAM, 0);
    if (sock_fd < 0) {
        FLOGE("socket udp socket error. [%s(%d)]", strerror(errno), errno);
        return -1;
    }
    memset(&addr_in, 0, sizeof(struct sockaddr_in));
    addr_in.sin_family = AF_INET;
    addr_in.sin_port = htons(0);
    addr_in.sin_addr.s_addr = htonl(INADDR_ANY);
    int32_t ret = bind(sock_fd, (struct sockaddr *) &addr_in, sizeof(addr_in));
    if (ret < 0) {
        FLOGE("bind udp socket error. [%s(%d)]", strerror(errno), errno);
        close(sock_fd);
        sock_fd = -1;
        return -1;
    }
    int32_t addrLen = 0;
    getsockname(sock_fd, (struct sockaddr *) &addr_in, (socklen_t *) &addrLen);
    *port = ntohs(addr_in.sin_port);
    return sock_fd;
}

void RtspClient::onSetupRequest(const char *data, int32_t cseq) {
    char temp[128] = {0};
    std::string response = "RTSP/1.0 200 OK\r\n";
    appendCommonResponse(&response, cseq);
    if (strncmp(strstr((const char *) data, "RTP/AVP"), "RTP/AVP/TCP", 11) == 0) {
        int32_t track1 = 0;
        int32_t track2 = 1;
        const char *str = strstr((const char *) data, "interleaved=");
        if (str) sscanf(str, "interleaved=%d-%d", &track1, &track2);
        is_use_tcp = true;
        memset(temp, 0, strlen(temp));
        sprintf(temp, "Transport: RTP/AVP/TCP;unicast;interleaved=%d-%d\r\n", track1, track2);
        response.append(temp);
    } else {
        is_use_tcp = false;

        int32_t rtp_port_s = 0;
        int32_t rtcp_port_s = 1;
        int32_t rtp_port_c = 2;
        int32_t rtcp_port_c = 3;

        const char *str = strstr((const char *) data, "client_port=");
        if (str) sscanf(str, "client_port=%d-%d", &rtp_port_c, &rtcp_port_c);
        //video
        if (strstr((const char *) data, "track1")) {
            v_rtp_socket = createUdpSocket(&rtp_port_s);
            if (v_rtp_socket <= 0) return;
            v_rtcp_socket = createUdpSocket(&rtcp_port_s);
            if (v_rtcp_socket <= 0) return;
            v_rtp_addrLen = sizeof(v_rtp_addr_in);
            getpeername(mSocket, (struct sockaddr *) &v_rtp_addr_in, (socklen_t *) &v_rtp_addrLen);
            v_rtp_addr_in.sin_port = htons(rtp_port_c);
            v_rtcp_addrLen = sizeof(v_rtcp_addr_in);
            getpeername(mSocket, (struct sockaddr *) &v_rtcp_addr_in,
                        (socklen_t *) &v_rtcp_addrLen);
            v_rtcp_addr_in.sin_port = htons(rtcp_port_c);
            vrtp_t = new std::thread(&RtspClient::videoRtpThread, this);
            SysUtil::setThreadName(vrtp_t, "RtspClient-v_u1");
            vrtcp_t = new std::thread(&RtspClient::videoRtcpThread, this);
            SysUtil::setThreadName(vrtcp_t, "RtspClient-v_u2");
        }
            //audio
        else {
            a_rtp_socket = createUdpSocket(&rtp_port_s);
            if (a_rtp_socket <= 0) return;
            a_rtcp_socket = createUdpSocket(&rtcp_port_s);
            if (a_rtcp_socket <= 0) return;
            a_rtp_addrLen = sizeof(a_rtp_addr_in);
            getpeername(mSocket, (struct sockaddr *) &a_rtp_addr_in, (socklen_t *) &a_rtp_addrLen);
            a_rtp_addr_in.sin_port = htons(rtp_port_c);
            a_rtcp_addrLen = sizeof(a_rtcp_addr_in);
            getpeername(mSocket, (struct sockaddr *) &a_rtcp_addr_in,
                        (socklen_t *) &a_rtcp_addrLen);
            a_rtcp_addr_in.sin_port = htons(rtcp_port_c);
            artp_t = new std::thread(&RtspClient::audioRtpThread, this);
            SysUtil::setThreadName(artp_t, "RtspClient-a_u1");
            artcp_t = new std::thread(&RtspClient::audioRtcpThread, this);
            SysUtil::setThreadName(artcp_t, "RtspClient-a_u2");
        }
        memset(temp, 0, strlen(temp));
        sprintf(temp, "Transport: RTP/AVP/UDP;unicast;client_port=%d-%d;server_port=%d-%d;\r\n", \
            rtp_port_c, rtcp_port_c, rtp_port_s, rtcp_port_s);
        response.append(temp);
    }
    memset(temp, 0, strlen(temp));
    sprintf(temp, "Session: %d\r\n", mSocket);
    response.append(temp);
    response.append("\r\n");
    int32_t ret = send(mSocket, response.c_str(), response.size(), 0);
    FLOGD("RtspClient send:len[%d], errno[%d]\n%s", ret, errno, response.c_str());
    if (ret <= 0) disconnect();
}

void RtspClient::onPlayRequest(const char *data, int32_t cseq) {
    std::string response = "RTSP/1.0 200 OK\r\n";
    appendCommonResponse(&response, cseq);
    response.append("Range: npt=0.000-\r\n");
    char temp[128];
    sprintf(temp, "Session: %d\r\n", mSocket);
    response.append(temp);
    response.append("\r\n");
    int32_t ret = send(mSocket, response.c_str(), response.size(), 0);
    FLOGD("RtspClient send:len[%d], errno[%d]\n%s", ret, errno, response.c_str());
    if (ret <= 0) disconnect();
}

void RtspClient::onGetParameterRequest(const char *data, int32_t cseq) {
    std::string response = "RTSP/1.0 200 OK\r\n";
    appendCommonResponse(&response, cseq);
    response.append("\r\n");
    int32_t ret = send(mSocket, response.c_str(), response.size(), 0);
    FLOGD("RtspClient send:len[%d], errno[%d]\n%s", ret, errno, response.c_str());
    if (ret <= 0) disconnect();
}

void RtspClient::onOtherRequest(const char *data, int32_t cseq) {
    std::string response = "RTSP/1.0 200 OK\r\n";
    appendCommonResponse(&response, cseq);
    response.append("\r\n");
    int32_t ret = send(mSocket, response.c_str(), response.size(), 0);
    FLOGD("RtspClient send:len[%d], errno[%d]\n%s", ret, errno, response.c_str());
    if (ret <= 0) disconnect();
}

void RtspClient::sendVFrame(const char *avc, int32_t size, int64_t ptsUsec) {
    if (size <= 0 || is_stop) return;
    //TODO::时间戮设置不对
    int32_t pts = ptsUsec * 90 / 1000;
    unsigned char nalu = avc[0];
    //hevc don't support splite data
    int32_t fau_num = is_hevc ? 1280 * 720 * 3 / 2 : 1280 - 18;
    if (size <= fau_num) {
        sequencenumber1++;
        char rtp_pack[16];
        rtp_pack[0] = '$';
        rtp_pack[1] = 0x00;
        ByteUtil::int16ToData(rtp_pack + 2, size + 12);
        rtp_pack[4] = 0x80;
        rtp_pack[5] = 0x60;
        ByteUtil::int16ToData(rtp_pack + 6, sequencenumber1);
        ByteUtil::int32ToData(rtp_pack + 8, pts);
        rtp_pack[12] = 0x01;
        rtp_pack[13] = 0x02;
        rtp_pack[14] = 0x03;
        rtp_pack[15] = 0x04;
        char send_pack[16 + size];
        memcpy(send_pack, rtp_pack, 16);
        memcpy(send_pack + 16, avc, size);
        if (is_use_tcp) {
            sendData(send_pack, size + 16);
        } else {
            sendto(v_rtp_socket, send_pack + 4, 12 + size, 0, (struct sockaddr *) &v_rtp_addr_in,
                   v_rtp_addrLen);
        }
    } else {
        int32_t num = 0;
        while ((size - 1 - num * fau_num) > 0) {
            bool first = (num == 0);
            bool last = ((size - 1 - num * fau_num) <= fau_num);
            int32_t rtpsize = last ? (size - 1 - num * fau_num) : fau_num;
            sequencenumber1++;
            char rtp_pack[18];
            rtp_pack[0] = '$';
            rtp_pack[1] = 0x00;
            ByteUtil::int16ToData(rtp_pack + 2, rtpsize + 14);
            rtp_pack[4] = 0x80;
            rtp_pack[5] = 0x60;
            ByteUtil::int16ToData(rtp_pack + 6, sequencenumber1);
            ByteUtil::int32ToData(rtp_pack + 8, pts);
            rtp_pack[12] = 0x01;
            rtp_pack[13] = 0x02;
            rtp_pack[14] = 0x03;
            rtp_pack[15] = 0x04;
            rtp_pack[16] = (nalu & 0xE0) | 0x1C;
            rtp_pack[17] = first ? (0x80 | (nalu & 0x1F)) : (last ? (0x40 | (nalu & 0x1F)) : (nalu &
                                                                                              0x1F));
            char send_pack[18 + size];
            memcpy(send_pack, rtp_pack, 18);
            memcpy(send_pack + 18, avc + num * fau_num + 1, rtpsize);
            if (is_use_tcp) {
                sendData(send_pack, rtpsize + 18);
            } else {
                sendto(v_rtp_socket, send_pack + 4, 14 + rtpsize, 0,
                       (struct sockaddr *) &v_rtp_addr_in, v_rtp_addrLen);
            }
            num++;
        }
    }
}

void RtspClient::sendAFrame(const char *aac, int32_t size, int64_t ptsUsec) {
    if (size <= 0 || is_stop) return;
    //TODO::时间戮设置不对
    int32_t pts = ptsUsec * 48 / 1000;
    sequencenumber2++;
    char rtp_pack[20];
    rtp_pack[0] = '$';
    rtp_pack[1] = 0x02;
    ByteUtil::int16ToData(rtp_pack + 2, size + 16);
    rtp_pack[4] = 0x80;
    rtp_pack[5] = 0x61;
    ByteUtil::int16ToData(rtp_pack + 6, sequencenumber2);
    ByteUtil::int32ToData(rtp_pack + 8, pts);
    rtp_pack[12] = 0x04;
    rtp_pack[13] = 0x03;
    rtp_pack[14] = 0x02;
    rtp_pack[15] = 0x01;
    rtp_pack[16] = 0x00;
    rtp_pack[17] = 0x10;
    rtp_pack[18] = ((size + 0) & 0x1FE0) >> 5;
    rtp_pack[19] = ((size + 0) & 0x1F) << 3;
    char send_pack[20 + size];
    memcpy(send_pack, rtp_pack, 20);
    memcpy(send_pack + 20, aac, size);
    if (is_use_tcp) {
        sendData(send_pack, size + 20);
    } else {
        sendto(a_rtp_socket, send_pack + 4, 16 + size, 0, (struct sockaddr *) &a_rtp_addr_in,
               a_rtp_addrLen);
    }
}

void RtspClient::selfFixedThread() {

}
