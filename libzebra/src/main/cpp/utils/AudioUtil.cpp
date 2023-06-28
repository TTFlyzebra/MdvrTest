//
// Created by Administrator on 2022/3/4.
//

#include "AudioUtil.h"

#include <system/audio.h>
extern "C" {
#include <libavformat/avformat.h>
#include <libswresample/swresample.h>
#include <libavutil/opt.h>
}

int32_t AudioUtil::getChannelsF(int32_t channel)
{
	switch (channel)
	{
	case AV_CH_LAYOUT_STEREO:
		return 2;
	case AV_CH_LAYOUT_MONO:
	default:
		return 1;
	}
}

int32_t AudioUtil::getFrameSizeF(int32_t format, int32_t channel)
{
	switch (format)
	{
	case AV_SAMPLE_FMT_U8:
	case AV_SAMPLE_FMT_U8P:
		return getChannelsF(channel) * 1;
	case AV_SAMPLE_FMT_S16:
	case AV_SAMPLE_FMT_S16P:
		return getChannelsF(channel) * 2;
	case AV_SAMPLE_FMT_S32:
	case AV_SAMPLE_FMT_FLT:
	case AV_SAMPLE_FMT_S32P:
	case AV_SAMPLE_FMT_FLTP:
		return getChannelsF(channel) * 4;
	case AV_SAMPLE_FMT_DBL:
	case AV_SAMPLE_FMT_DBLP:
	case AV_SAMPLE_FMT_S64:
	case AV_SAMPLE_FMT_S64P:
		return getChannelsF(channel) * 8;
	default:
		return getChannelsF(channel) * 2;
	}
}

int32_t AudioUtil::getSampleSizeF(int32_t format)
{
	switch (format)
	{
	case AV_SAMPLE_FMT_U8:
	case AV_SAMPLE_FMT_U8P:
		return 8;
	case AV_SAMPLE_FMT_S16:
	case AV_SAMPLE_FMT_S16P:
		return 16;
	case AV_SAMPLE_FMT_S32:
	case AV_SAMPLE_FMT_FLT:
	case AV_SAMPLE_FMT_S32P:
	case AV_SAMPLE_FMT_FLTP:
		return 32;
	case AV_SAMPLE_FMT_DBL:
	case AV_SAMPLE_FMT_DBLP:
	case AV_SAMPLE_FMT_S64:
	case AV_SAMPLE_FMT_S64P:
		return 32;
	default:
		return 16;
	}
}

int64_t AudioUtil::getFchannelFromA(int32_t channel)
{
    switch(channel){
        case AUDIO_CHANNEL_IN_STEREO:
        case AUDIO_CHANNEL_OUT_STEREO:
            return AV_CH_LAYOUT_STEREO;
        case AUDIO_CHANNEL_IN_MONO:
        case AUDIO_CHANNEL_OUT_MONO:
        default:
            return AV_CH_LAYOUT_MONO;
    }
}

int32_t AudioUtil::getChannelsA(int32_t channel)
{
    int32_t count = 1;
    switch(channel){
        case AUDIO_CHANNEL_IN_STEREO:
        case AUDIO_CHANNEL_OUT_STEREO:
            count = 2;
            break;
        case AUDIO_CHANNEL_IN_MONO:
        case AUDIO_CHANNEL_OUT_MONO:
        default:
            count = 1;
            break;
    }
    return count;
}

int32_t AudioUtil::getFrameSizeA(int32_t format, int32_t channel)
{
    switch(format){
        case AUDIO_FORMAT_PCM_8_BIT:
            return 1 * getChannelsA(channel);
        case AUDIO_FORMAT_PCM_16_BIT:
            return 2 * getChannelsA(channel);
        case AUDIO_FORMAT_PCM_24_BIT_PACKED:
            return 3 * getChannelsA(channel);
        case AUDIO_FORMAT_PCM_8_24_BIT:
            return 4 * getChannelsA(channel);
        case AUDIO_FORMAT_PCM_32_BIT:
            return 4 * getChannelsA(channel);
        case AUDIO_FORMAT_PCM_FLOAT:
            return 4 * getChannelsA(channel);
        default:
            return 2 * getChannelsA(channel);
    }
}