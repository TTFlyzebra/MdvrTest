//
// Created by Administrator on 2022/2/11.
//

#ifndef ANDROID_AUDIOUTIL_H
#define ANDROID_AUDIOUTIL_H

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswresample/swresample.h>
#include <libavutil/opt.h>
}

class AudioUtil {
public:
    static int32_t getAudioChannels(int32_t channel_layout);
    static int32_t getAudioFrameSize(int32_t format, int32_t channel_layout);
    static int32_t getAudioSampleSize(int32_t format);
};

#endif //ANDROID_AUDIOUTIL_H
