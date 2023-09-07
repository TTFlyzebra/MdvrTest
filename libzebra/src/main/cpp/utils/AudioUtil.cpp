//
//Created by Administrator on 2022/2/11.
//

#include "AudioUtil.h"

int32_t AudioUtil::getAudioChannels(int32_t channel_layout)
{
	switch (channel_layout)
	{
	case AV_CH_LAYOUT_STEREO:
		return 2;
	case AV_CH_LAYOUT_MONO:
	default:
		return 1;
	}	
}

int32_t AudioUtil::getAudioFrameSize(int32_t format, int32_t channel_layout)
{
	switch (format)
	{
	case AV_SAMPLE_FMT_U8:
	case AV_SAMPLE_FMT_U8P:
		return getAudioChannels(channel_layout) * 1;
	case AV_SAMPLE_FMT_S16:
	case AV_SAMPLE_FMT_S16P:
		return getAudioChannels(channel_layout) * 2;
	case AV_SAMPLE_FMT_S32:
	case AV_SAMPLE_FMT_FLT:
	case AV_SAMPLE_FMT_S32P:
	case AV_SAMPLE_FMT_FLTP:
		return getAudioChannels(channel_layout) * 4;
	case AV_SAMPLE_FMT_DBL:
	case AV_SAMPLE_FMT_DBLP:
	case AV_SAMPLE_FMT_S64:
	case AV_SAMPLE_FMT_S64P:
		return getAudioChannels(channel_layout) * 8;
	default:
		return getAudioChannels(channel_layout) * 2;
	}
}

int32_t AudioUtil::getAudioSampleSize(int32_t format)
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