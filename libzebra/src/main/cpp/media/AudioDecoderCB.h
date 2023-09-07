#ifndef ANDROID_IAUDIODECODECB_H
#define ANDROID_IAUDIODECODECB_H

#include <stdint.h>

class AudioDecoderCB {
public:
    virtual ~AudioDecoderCB() {};
    virtual void notifyPcmData(const char* data, int32_t size, int32_t sample, int32_t channel, int32_t format, int64_t pts) = 0;
};

#endif //ANDROID_IAUDIODECODECB_H