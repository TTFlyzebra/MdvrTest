//
// Created by Administrator on 2023/6/23.
//

#include <cstdio>
#include <unistd.h>
#include "RtmpDump.h"
#include "buffer/BufferManager.h"
#include "buffer/LoopBuf.h"
#include "utils/FlyLog.h"
#include "librtmp/rtmp.h"

RtmpDump::RtmpDump(JavaVM *jvm, JNIEnv *env, jobject thiz)
        : rtmp(nullptr) {
    callBack = new CallBack(jvm, env, thiz);
}

RtmpDump::~RtmpDump() {
    delete callBack;
}

bool RtmpDump::open(int channel, const char *url) {
    mChannel = channel;

    memset(rtmp_url, 0, sizeof(rtmp_url));
    memcpy(rtmp_url, url, strlen(url));

    rtmp = RTMP_Alloc();
    if (rtmp == nullptr) {
        FLOGE("RTMP_Alloc failed");
        return false;
    }
    RTMP_Init(rtmp);
    rtmp->Link.timeout = 5;
    rtmp->Link.lFlags |= RTMP_LF_LIVE;
    int ret = RTMP_SetupURL(rtmp, rtmp_url);
    if (ret == FALSE) {
        RTMP_Free(rtmp);
        rtmp = nullptr;
        FLOGE("RTMP_SetupURL %s failed. ret=%d", rtmp_url, ret);
        return false;
    }
    RTMP_EnableWrite(rtmp);
    ret = RTMP_Connect(rtmp, nullptr);
    if (!ret) {
        RTMP_Free(rtmp);
        rtmp = nullptr;
        FLOGE("RTMP_Connect ret=%d", ret);
        return false;
    }

    ret = RTMP_ConnectStream(rtmp, 0);
    if (!ret) {
        ret = RTMP_ConnectStream(rtmp, 0);
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
        rtmp = nullptr;
        FLOGE("RTMP_ConnectStream ret=%d", ret);
        return false;
    }

    return true;
}

void RtmpDump::close() {
    if (rtmp != nullptr) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
        rtmp = nullptr;
    }
}

bool RtmpDump::sendAacHead(const char *head, int headLen) {
    auto *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Reset(packet);
    int bodySize = 2 + headLen;
    int ret = RTMPPacket_Alloc(packet, bodySize);
    if (!ret) {
        callBack->javaOnError(-2);
        return FALSE;
    }
    // SoundFormat(4bits):10=AAC；
    // SoundRate(2bits):3=44kHz；
    // SoundSize(1bit):1=16-bit samples；
    // SoundType(1bit):1=Stereo sound；
    packet->m_body[0] = 0xAE;
    // 1表示AAC raw，
    // 0表示AAC sequence header
    packet->m_body[1] = 0x00;
    memcpy(&packet->m_body[2], head, headLen);

    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nBodySize = bodySize;
    packet->m_nChannel = 0x14 + mChannel;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nTimeStamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;

    ret = RTMP_SendPacket(rtmp, packet, 0);
    if (ret == FALSE) {
        FLOGE("sendAacHead[%d] failed errno[%d]!", mChannel, errno);
    }
    RTMPPacket_Free(packet);
    free(packet);
    return ret;
}

bool RtmpDump::sendAacData(const char *data, int size, long pts) {
    auto *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Reset(packet);
    int bodySize = 2 + size;
    int ret = RTMPPacket_Alloc(packet, bodySize);
    if (!ret) {
        callBack->javaOnError(-2);
        return FALSE;
    }
    // SoundFormat(4bits):10=AAC；
    // SoundRate(2bits):3=44kHz；
    // SoundSize(1bit):1=16-bit samples；
    // SoundType(1bit):1=Stereo sound；
    packet->m_body[0] = 0xAE;
    // 1表示AAC raw，
    // 0表示AAC sequence header
    packet->m_body[1] = 0x01;
    memcpy(&packet->m_body[2], data, size);

    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nBodySize = bodySize;
    packet->m_nChannel = 0x14 + mChannel;
    packet->m_hasAbsTimestamp = pts / 1000;
    packet->m_nTimeStamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;

    ret = RTMP_SendPacket(rtmp, packet, 0);
    if (ret == FALSE) {
        FLOGE("sendAacData[%d] failed errno[%d]!", mChannel, errno);
    }
    RTMPPacket_Free(packet);
    free(packet);
    return ret;
}

bool RtmpDump::sendAvcHead(const char *data, int size) {
    int sps_ptr = -1;
    int pps_ptr = -1;
    for (int i = 0; i < size - 4; i++) {
        if (data[i] == 0x00 && data[i + 1] == 0x00 && data[i + 2] == 0x00 && data[i + 3] == 0x01) {
            if (sps_ptr == -1) {
                sps_ptr = i;
                i += 3;
            } else {
                pps_ptr = i;
                break;
            }
        }
    }
    if (sps_ptr == -1 || pps_ptr == -1) {
        FLOGE("Get sps pps error!");
        return false;
    }
    int sps_len = pps_ptr - 4;
    const char *sps = data + sps_ptr + 4;
    int pps_len = size - pps_ptr - 4;
    const char *pps = data + pps_ptr + 4;

    auto *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Reset(packet);
    int ret = RTMPPacket_Alloc(packet, 2 + 3 + 5 + 1 + 2 + sps_len + 1 + 2 + pps_len);
    if (!ret) {
        callBack->javaOnError(-1);
        return FALSE;
    }
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = 2 + 3 + 5 + 1 + 2 + sps_len + 1 + 2 + pps_len;
    packet->m_nChannel = 0x04 + mChannel;
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    //packet->m_nInfoField2 = rtmp->m_stream_id;
    int i = 0;
    packet->m_body[i++] = 0x17;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    //AVCDecoderConfigurationRecord
    packet->m_body[i++] = 0x01;
    packet->m_body[i++] = sps[1];
    packet->m_body[i++] = sps[2];
    packet->m_body[i++] = sps[3];
    packet->m_body[i++] = 0xff;
    //sps
    packet->m_body[i++] = 0xe1;
    packet->m_body[i++] = (sps_len >> 8) & 0xff;
    packet->m_body[i++] = (sps_len) & 0xff;
    memcpy(packet->m_body + i, sps, sps_len);
    i += sps_len;
    //pps
    packet->m_body[i++] = 0x01;
    packet->m_body[i++] = (pps_len >> 8) & 0xff;
    packet->m_body[i++] = (pps_len) & 0xff;
    memcpy(packet->m_body + i, pps, pps_len);

    ret = RTMP_SendPacket(rtmp, packet, 0);
    if (ret == FALSE) {
        FLOGE("sendAvcHead[%d] failed errno[%d]!", mChannel, errno);
    }
    RTMPPacket_Free(packet);
    free(packet);
    return ret;
}

bool RtmpDump::sendAvcData(const char *data, int size, long pts) {
    auto *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Reset(packet);
    int ret = RTMPPacket_Alloc(packet, 9 + size);
    if (!ret) {
        callBack->javaOnError(-2);
        return FALSE;
    }
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = 9 + size;
    packet->m_nChannel = 0x04 + mChannel;
    packet->m_nTimeStamp = pts / 1000;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    //packet->m_nInfoField2 	= rtmp->m_stream_id;
    memcpy(packet->m_body, (data[0] & 0x1f) != 1 ? "\x17\x01" : "\x27\x01", 2);
    memcpy(packet->m_body + 2, "\x00\x00\x00", 3);
    AMF_EncodeInt32(packet->m_body + 5, packet->m_body + 9, size);
    memcpy(packet->m_body + 9, data, size);

    ret = RTMP_SendPacket(rtmp, packet, 0);
    if (ret == FALSE) {
        FLOGE("sendAvcData[%d] failed errno[%d]!", mChannel, errno);
    }
    RTMPPacket_Free(packet);
    free(packet);
    return ret;
}

bool RtmpDump::sendHevcHead(const char *data, int size) {
    int vps_p = -1;
    int sps_p = -1;
    int pps_p = -1;
    for (int i = 0; i < size - 4; i++) {
        if (data[i] == 0x00 && data[i + 1] == 0x00 && data[i + 2] == 0x00 && data[i + 3] == 0x01) {
            if (vps_p == -1) {
                vps_p = i;
                i += 3;
            } else if (sps_p == -1) {
                sps_p = i;
                i += 3;
            } else {
                pps_p = i;
                break;
            }
        }
    }
    if (vps_p == -1 || sps_p == -1 || pps_p == -1) {
        FLOGE("Get vps sps pps error!");
        return false;
    }
    int vps_len = sps_p - 4;
    const char *vps = data + vps_p + 4;
    int sps_len = pps_p - sps_p - 4;
    const char *sps = data + sps_p + 4;
    int pps_len = size - pps_p - 4;
    const char *pps = data + pps_p + 4;

    auto *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Reset(packet);
    int ret = RTMPPacket_Alloc(packet, 43 + vps_len + sps_len + pps_len);
    if (!ret) {
        callBack->javaOnError(-1);
        return FALSE;
    }
    //packet->m_nInfoField2 = rtmp->m_stream_id;
    int i = 0;
    packet->m_body[i++] = 0x1c;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x01;
    packet->m_body[i++] = sps[6];
    packet->m_body[i++] = sps[7];
    packet->m_body[i++] = sps[8];
    packet->m_body[i++] = sps[9];
    packet->m_body[i++] = sps[12];
    packet->m_body[i++] = sps[13];
    packet->m_body[i++] = sps[14];
    //48 bit nothing deal in rtmp
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    //bit(16) avgFrameRate
    /* bit(2) constantFrameRate; */
    /* bit(3) numTemporalLayers; */
    /* bit(1) temporalIdNested; */
    packet->m_body[i++] = 0x83;
    /*unsigned int(8) numOfArrays; 03*/
    packet->m_body[i++] = 0x03;
    //vps 32
    packet->m_body[i++] = 0x20;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x01;
    packet->m_body[i++] = (vps_len >> 8) & 0xff;
    packet->m_body[i++] = (vps_len) & 0xff;
    memcpy(&packet->m_body[i], vps, vps_len);
    i += vps_len;
    //sps
    packet->m_body[i++] = 0x21;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x01;
    packet->m_body[i++] = (sps_len >> 8) & 0xff;
    packet->m_body[i++] = (sps_len) & 0xff;
    memcpy(&packet->m_body[i], sps, sps_len);
    i += sps_len;
    //pps
    packet->m_body[i++] = 0x22;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x01;
    packet->m_body[i++] = (pps_len >> 8) & 0xff;
    packet->m_body[i++] = (pps_len) & 0xff;
    memcpy(&packet->m_body[i], pps, pps_len);
    i += pps_len;
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = i;
    packet->m_nChannel = 0x04 + mChannel;
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;

    ret = RTMP_SendPacket(rtmp, packet, 0);
    if (ret == FALSE) {
        FLOGE("sendHevcHead[%d] failed errno[%d]!", mChannel, errno);
    }
    RTMPPacket_Free(packet);
    free(packet);
    return ret;
}

bool RtmpDump::sendHevcData(const char *data, int size, long pts) {
    auto *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Reset(packet);
    int ret = RTMPPacket_Alloc(packet, 9 + size);
    if (!ret) {
        callBack->javaOnError(-2);
        return FALSE;
    }
    int i = 0;
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = 9 + size;
    packet->m_nChannel = 0x04 + mChannel;
    packet->m_nTimeStamp = pts / 1000;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    //packet->m_nInfoField2 = rtmp->m_stream_id;
    if (((data[0] & 0x7e) >> 1) == 19) {
        packet->m_body[i++] = 0x1c;
    } else {
        packet->m_body[i++] = 0x2c;
    }
    packet->m_body[i++] = 0x01;//AVC NALU
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    //NALU size
    packet->m_body[i++] = (size >> 24) & 0xff;
    packet->m_body[i++] = (size >> 16) & 0xff;
    packet->m_body[i++] = (size >> 8) & 0xff;
    packet->m_body[i++] = (size) & 0xff;
    memcpy(&packet->m_body[i], data, size);

    ret = RTMP_SendPacket(rtmp, packet, 0);
    if (ret == FALSE) {
        FLOGE("sendHevcData[%d] failed errno[%d]!", mChannel, errno);
    }
    RTMPPacket_Free(packet);
    free(packet);
    return ret;
}
