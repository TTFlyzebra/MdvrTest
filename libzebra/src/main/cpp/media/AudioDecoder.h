#ifndef ANDROID_AudioDecoder_H
#define ANDROID_AudioDecoder_H

#include <stdint.h>
#include <vector>
#include <mutex>
#include <thread>
#include <condition_variable>

class AudioDecoderCB;

class AudioDecoder
{
public:
    AudioDecoder(AudioDecoderCB* callback, const char* tag);
    ~AudioDecoder();

    void initCodec(const char* name, int32_t sample, int32_t channel, int32_t format);
    void inAacData(const char* data, int32_t size, uint32_t pts);
    void releaseCodec();

private:
    void outPcmData();

private:
    const char* mTag;
    const struct AVCodec* a_codec;
    struct AVCodecContext* a_ctx;
    struct AVPacket* a_packet;
    struct AVFrame* a_frame;
    struct SwrContext* swr_ctx;

    AudioDecoderCB* mCallback;
    bool is_codec_init;

    int32_t mSample;
    int32_t mChannel;
    int32_t mFormat;

    std::thread* out_t;
    std::mutex mlock_out;
    uint8_t* out_pcm;
    std::vector<char> outBuf;
    std::condition_variable mcond_out;
};

#endif //ANDROID_AudioDecoder_H
