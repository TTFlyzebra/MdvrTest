//
// Created by Administrator on 2023/6/23.
//

#ifndef RTMPCAMERA_RTMPDUMP_H
#define RTMPCAMERA_RTMPDUMP_H

#include <jni.h>
#include <queue>
#include <mutex>
#include <thread>
#include <condition_variable>
#include "RtmpDumpCB.h"

class RTMP;

class RTMPPacket;

class RtmpDump {
public:
    RtmpDump(JavaVM *jvm, JNIEnv *env, jobject thiz);

    ~RtmpDump();

    bool open(int channel, const char *url);

    void close();

    bool sendAacHead(const char *head, int size);

    bool sendAacData(const char *data, int size, long pts);

    bool sendAvcHead(const char *data, int size);

    bool sendAvcData(const char *data, int size, long pts);

    bool sendHevcHead(const char *data, int size);

    bool sendHevcData(const char *data, int size, long pts);

private:
    RtmpDumpCB *cb;
    int mChannel;
    char rtmp_url[1024];
    RTMP *rtmp;
};


#endif //RTMPCAMERA_RTMPDUMP_H
