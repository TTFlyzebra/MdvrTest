#include "VideoDecoder.h"

#include "VideoDecoderCB.h"
#include "utils/FlyLog.h"
#include "Config.h"
#include "rfc/Protocol.h"
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
}
#include <libyuv.h>

VideoDecoder::VideoDecoder(VideoDecoderCB* callback,  const char* tag)
        : mTag(tag)
        , v_ctx(nullptr)
        , v_packet(nullptr)
        , v_frame(nullptr)
        , mCallback(callback)
        , is_codec_init(false)
        , out_t(nullptr)
        , out_width(0)
        , out_height(0)
        , out_size(0)
{
    FLOGD("%s->%s()", mTag, __func__);
}

VideoDecoder::~VideoDecoder()
{
    FLOGD("%s->%s()", mTag, __func__);
    releaseCodec();
    mCallback = nullptr;
}

void VideoDecoder::initCodec(const char* name, int32_t width, int32_t height)
{
    FLOGD("%s->VideoDecoder:%s()", mTag, __func__);
    mWidth = width;
    mHeight = height;
    av_register_all();
    avformat_network_init();
    v_codec = avcodec_find_decoder_by_name(name);
    if (!v_codec) {
        FLOGE("%s->avcodec_find_decoder failed.", mTag);
        return;
    }
    if (!(v_ctx = avcodec_alloc_context3(v_codec))) {
        FLOGE("%s->avcodec_alloc_context3 failed.", mTag);
        return;
    }
    if (avcodec_open2(v_ctx, v_codec, nullptr) < 0) {
        FLOGE("%s->avcodec_open2 failed.", mTag);
        return;
    }
    if (!(v_packet = av_packet_alloc())) {
        FLOGE("%s->av_packet_alloc failed.", mTag);
        return;
    }
    if (!(v_frame = av_frame_alloc())) {
        FLOGE("%s->av_frame_alloc failed", mTag);
        return;
    }
    {
        std::lock_guard<std::mutex> lock_codec(mlock_codec);
        is_codec_init = true;
    }
    out_t = new std::thread(&VideoDecoder::outYuvData, this);
    FLOGD("%s->VideoDecoder:%s() exit", mTag, __func__);
}

void VideoDecoder::inAvcData(const char* data, int32_t size)
{
    std::lock_guard<std::mutex> lock_codec(mlock_codec);
    if (!is_codec_init) {
        FLOGE("%s->video decoder not init!", mTag);
        return;
    }
    v_packet->data = (uint8_t*)data;
    v_packet->size = size;
    int32_t ret = avcodec_send_packet(v_ctx, v_packet);
    if (ret < 0) {
        FLOGD("%s->avcodec_send_packet error! ret[%d]", mTag, ret);
        char log[256] = { 0 };
        int32_t num = size < 48 ? size : 48;
        for (int32_t i = 0; i < num; i++) {
            sprintf(log, "%s%02x:", log, data[i] & 0xFF);
        }
        FLOGD("%s->%s[%d]", mTag, log, size);
        return;
    }
    while (ret >= 0) {
        ret = avcodec_receive_frame(v_ctx, v_frame);
        if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
            break;
        }
        else if (ret < 0) {
            FLOGE("%s->avcodec_receive_frame error! ret[%d]", mTag, ret);
            size = 0;
            break;
        }
        int32_t width = v_frame->width;
        int32_t height = v_frame->height;
        int32_t yuvSize = width * height * 3 / 2;
        libyuv::I420Copy(
                v_frame->data[0],
                v_frame->linesize[0],
                v_frame->data[1],
                v_frame->linesize[1],
                v_frame->data[2],
                v_frame->linesize[2],
                (uint8_t*)frame_data,
                v_frame->linesize[0],
                (uint8_t*)frame_data + v_frame->linesize[0] * height,
                v_frame->linesize[1],
                (uint8_t*)frame_data + v_frame->linesize[0] * height + v_frame->linesize[1] * height / 2,
                v_frame->linesize[2],
                width,
                height);
        {
            std::lock_guard<std::mutex> lock_out(mlock_out);
            libyuv::ConvertToI420(
                    (uint8_t*)frame_data,
                    0,
                    (uint8_t*)yuv_data,
                    width,
                    (uint8_t*)yuv_data + width * height,
                    width / 2,
                    (uint8_t*)yuv_data + width * height * 5 / 4,
                    width / 2,
                    0,
                    0,
                    v_frame->linesize[0],
                    height,
                    width,
                    height,
                    libyuv::kRotate0,
                    libyuv::FOURCC_I420);
            out_width = width;
            out_height = height;
            out_size = yuvSize;
            mcond_out.notify_one();
        }
        av_frame_unref(v_frame);
    }
    av_packet_unref(v_packet);
}

void VideoDecoder::releaseCodec()
{
    FLOGD("%s->VideoDecoder:%s()", mTag, __func__);
    {
        std::lock_guard<std::mutex> lock_codec(mlock_codec);
        if (!is_codec_init) {
            FLOGD("%s->VideoDecoder codec is already release!", mTag);
            return;
        }
        is_codec_init = false;
    }
    {
        std::lock_guard<std::mutex> lock_out(mlock_out);
        mcond_out.notify_all();
    }
    if (out_t) {
        out_t->join();
        delete out_t;
        out_t = nullptr;
    }
    if (v_packet) {
        //FLOGD("%s->av_packet_free v_packet.", mTag);
        av_packet_free(&v_packet);
        v_packet = nullptr;
    }
    if (v_frame) {
        //FLOGD("%s->av_frame_free v_frame.", mTag);
        av_frame_free(&v_frame);
        v_frame = nullptr;
    }
    if (v_codec) {
        //FLOGD("%s->avcodec_close v_ctx.", mTag);
        avcodec_close(v_ctx);
    }
    if (v_ctx) {
        //FLOGD("%s->avcodec_free_context v_ctx.", mTag);
        avcodec_free_context(&v_ctx);
        v_ctx = nullptr;
    }
    FLOGD("%s->VideoDecoder:%s() exit", mTag, __func__);
}

void  VideoDecoder::outYuvData()
{
    int32_t width, height, size;
    char* out_data = (char*)malloc(4096 * 4096 * 3 / 2 * sizeof(char));
    if (!out_data) return;
    while (is_codec_init) {
        {
            std::unique_lock<std::mutex> lock_out(mlock_out);
            while (is_codec_init && out_size == 0) {
                mcond_out.wait(lock_out);
            }
            if (!is_codec_init || out_size == 0) break;
            memcpy(out_data, yuv_data, out_size);
            width = out_width;
            height = out_height;
            size = out_size;
            out_size = 0;
        }
        if (mCallback) mCallback->notifyYuvData(out_data, size, width, height, 0);
    }
    free(out_data);
}
