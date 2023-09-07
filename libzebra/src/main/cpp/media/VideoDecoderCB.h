#ifndef ANDROID_IVIDEODECODECB_H
#define ANDROID_IVIDEODECODECB_H

#include <stdint.h>

class VideoDecoderCB {
public:
    virtual ~VideoDecoderCB() {};
    virtual void notifyYuvData(const char* data, int32_t size, int32_t width, int32_t height, int64_t pts) = 0;
};

#endif //ANDROID_IVIDEODECODECB_H
