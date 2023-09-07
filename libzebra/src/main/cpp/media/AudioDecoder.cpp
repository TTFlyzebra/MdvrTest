#include "AudioDecoder.h"

#include "AudioDecoderCB.h"
#include "utils/FlyLog.h"
#include "utils/SysUtil.h"
#include "utils/AudioUtil.h"
#include "Config.h"
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswresample/swresample.h>
#include <libavutil/opt.h>
}

AudioDecoder::AudioDecoder(AudioDecoderCB* callback, const char* tag)
    : mTag(tag)
    , a_ctx(nullptr)
    , a_packet(nullptr)
    , a_frame(nullptr)
    , swr_ctx(nullptr)
    , mCallback(callback)
    , is_codec_init(false)
    , out_t(nullptr)
    , out_pcm(nullptr)
{
    FLOGD("%s->%s()", mTag, __func__);
}

AudioDecoder::~AudioDecoder()
{
    releaseCodec();
    mCallback = nullptr;
    FLOGD("%s->%s()", mTag, __func__);
}

void AudioDecoder::initCodec(const char* name, int32_t sample, int32_t channel, int32_t format)
{
    FLOGD("%s->AudioDecoder:%s()", mTag, __func__);
    mSample = sample;
    mChannel = channel;
    mFormat = format;
    av_register_all();
    avformat_network_init();
    a_codec = avcodec_find_decoder_by_name("aac");
    if (!a_codec) {
        FLOGE("%s->avcodec_find_decoder_by_name failed.", mTag);
        return;
    }
    if (!(a_ctx = avcodec_alloc_context3(a_codec))) {
        FLOGE("%s->avcodec_alloc_context3 failed.", mTag);
        return;
    }
    a_ctx->sample_rate = mSample;
    a_ctx->channel_layout = mChannel;
    int32_t channels = av_get_channel_layout_nb_channels(mChannel);
    a_ctx->channels = channels;

    if (avcodec_open2(a_ctx, a_codec, nullptr) < 0) {
        FLOGE("%s->avcodec_open2 failed.", mTag);
        return;
    }
    if (!(a_packet = av_packet_alloc())) {
        FLOGE("%s->av_packet_alloc failed.", mTag);
        return;
    }
    if (!(a_frame = av_frame_alloc())) {
        FLOGE("%s->av_frame_alloc failed.", mTag);
        return;
    }
    if (!av_samples_alloc(&out_pcm, NULL, channels, mSample, (enum AVSampleFormat)mFormat, 0)) {
        FLOGE("av_samples_alloc failed.");
        return;
    }
    is_codec_init = true;
    out_t = new std::thread(&AudioDecoder::outPcmData, this);
    char tname[32];
    sprintf(tname, "%sADecoder2", mTag);
    SysUtil::setThreadName(out_t, tname);
    FLOGD("%s->AudioDecoder:%s() exit", mTag, __func__);
}

void AudioDecoder::inAacData(const char* data, int32_t size, uint32_t pts)
{
    if (!is_codec_init) {
        FLOGE("%s->audio decoder not init!", mTag);
        return;
    }
    a_packet->data = (uint8_t*)data;
    a_packet->size = size;
    int32_t ret = avcodec_send_packet(a_ctx, a_packet);
    if (ret < 0) {
        FLOGE("%s->avcodec_send_packet error! ret[%d]", mTag, ret);
        char log[256] = { 0 };
        int32_t num = size < 48 ? size : 48;
        for (int32_t i = 0; i < num; i++) {
            sprintf(log, "%s%02x:", log, data[i] & 0xFF);
        }
        FLOGE("%s[%d]", log, size);
        return;
    }
    while (ret >= 0) {
        ret = avcodec_receive_frame(a_ctx, a_frame);
        if (ret < 0) break;
        if (!swr_ctx) {
            swr_ctx = swr_alloc();
            av_opt_set_int(swr_ctx, "in_channel_layout", a_frame->channel_layout, 0);
            av_opt_set_int(swr_ctx, "out_channel_layout", mChannel, 0);
            av_opt_set_int(swr_ctx, "in_sample_rate", a_frame->sample_rate, 0);
            av_opt_set_int(swr_ctx, "out_sample_rate", mSample, 0);
            av_opt_set_sample_fmt(swr_ctx, "in_sample_fmt", (AVSampleFormat)a_frame->format, 0);
            av_opt_set_sample_fmt(swr_ctx, "out_sample_fmt", (AVSampleFormat)mFormat, 0);
            swr_init(swr_ctx);
        }
        int64_t delay = swr_get_delay(swr_ctx, a_frame->sample_rate);
        int64_t out_count = av_rescale_rnd(
            a_frame->nb_samples + delay,
            mSample,
            a_frame->sample_rate,
            AV_ROUND_UP);
        int32_t retLen = swr_convert(
            swr_ctx,
            &out_pcm,
            out_count,
            (const uint8_t**)a_frame->data,
            a_frame->nb_samples);
        if (retLen > 0) {
            std::lock_guard<std::mutex> lock(mlock_out);
            outBuf.insert(outBuf.end(), out_pcm, out_pcm + retLen * AudioUtil::getAudioFrameSize(mFormat, mChannel));
            mcond_out.notify_one();
        }
    }
}

void AudioDecoder::releaseCodec()
{
    FLOGD("%s->AudioDecoder:%s()", mTag, __func__);
    if (!is_codec_init) {
        FLOGE("%s->AudioDecoder codec is already release!", mTag);
        return;
    }
    is_codec_init = false;
    {
        std::lock_guard<std::mutex> lock(mlock_out);
        mcond_out.notify_all();
    }
    if (out_t) {
        out_t->join();
        delete out_t;
        out_t = nullptr;
    }
    if (a_packet) {
        //FLOGD("av_packet_free packet.");
        av_packet_free(&a_packet);
    }
    if (a_frame) {
        //FLOGD("av_frame_free frame.");
        av_frame_free(&a_frame);
    }
    if (a_codec) {
        //FLOGD("avcodec_close a_ctx.");
        avcodec_close(a_ctx);
    }
    if (a_ctx) {
        //FLOGD("avcodec_free_context a_ctx.");
        avcodec_free_context(&a_ctx);
    }
    if (swr_ctx) {
        //FLOGD("swr_free swr_ctx.");
        swr_free(&swr_ctx);
    }
    if (out_pcm) {
        //FLOGD("av_free a_tempBuf.");
        av_freep(&out_pcm);
    }
    FLOGD("%s->AudioDecoder:%s() exit", mTag, __func__);
}

void  AudioDecoder::outPcmData()
{
    size_t out_size = 0;
    size_t max_size = 8192;
    char* out_data = (char*)malloc(max_size * sizeof(char));
    if (!out_data) return;
    while (is_codec_init) {
        {
            std::unique_lock<std::mutex> lock(mlock_out);
            while (is_codec_init && outBuf.empty()) {
                mcond_out.wait(lock);
            }
            if (!is_codec_init || outBuf.empty()) break;
            out_size = std::min(max_size, outBuf.size());
            memcpy(out_data, &outBuf[0], out_size);
            outBuf.erase(outBuf.begin(), outBuf.begin() + out_size);
        }
        if (mCallback) mCallback->notifyPcmData(out_data, out_size, mSample, mChannel, mFormat, 0);
    }
    if (out_data) free(out_data);
}
