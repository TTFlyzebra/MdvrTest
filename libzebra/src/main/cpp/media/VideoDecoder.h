#ifndef ANDROID_VIDEODECODER_H
#define ANDROID_VIDEODECODER_H

#include <stdint.h>
#include <vector>
#include <mutex>
#include <thread>
#include <condition_variable>

class VideoDecoderCB;

class VideoDecoder
{
public:
    VideoDecoder(VideoDecoderCB* callback,  const char* tag);
    ~VideoDecoder();

    void initCodec(const char* name, int32_t width, int32_t height);
    void inAvcData(const char* data, int32_t size);
    void releaseCodec();

private:
    void outYuvData();

private:
    const char* mTag;
    const struct AVCodec* v_codec;
    struct AVCodecContext* v_ctx;
    struct AVPacket* v_packet;
    struct AVFrame* v_frame;
    char frame_data[4096 * 4096 * 3 / 2];

    VideoDecoderCB* mCallback;
    bool is_codec_init;
    std::mutex mlock_codec;

    std::thread* out_t;
    int32_t out_width;
    int32_t out_height;
    int32_t out_size;
    std::mutex mlock_out;
    char yuv_data[4096 * 4096 * 3 / 2];
    std::condition_variable mcond_out;

    int32_t mWidth;
    int32_t mHeight;
};

#endif //ANDROID_VIDEODECODER_H
