//
// Created by Administrator on 2022/2/11.
//

#ifndef F_ZEBRA_AUDIOUTIL_H
#define F_ZEBRA_AUDIOUTIL_H

#include <stdint.h>

class AudioUtil {
public:
    static int32_t getChannelsF(int32_t channel);
    static int32_t getFrameSizeF(int32_t format, int32_t channel);
    static int32_t getSampleSizeF(int32_t format);
    static int64_t getFchannelFromA(int32_t channel);
    static int32_t getChannelsA(int32_t channel);
    static int32_t getFrameSizeA(int32_t format, int32_t channel);
};

#endif //F_ZEBRA_AUDIOUTIL_H
