//
// Created by Administrator on 2022/3/2.
//

#include "SndOutService.h"

extern "C" {
#include <libavformat/avformat.h>
#include <libswresample/swresample.h>
}
#include "utils/FlyLog.h"
#include "Config.h"
#include "rfc/Protocol.h"
#include "base/User.h"
#include "base/Terminal.h"
#include "media/AudioDecoder.h"
#include "utils/ByteUtil.h"
#include "utils/SysUtil.h"

SndOutService::SndOutService(Notify* notify, int64_t tid)
    : BaseNotify(notify)
    , mTid(tid)
    , mAudioDecoder(nullptr)
    , aac_t(nullptr)
    , is_sound(false)
{
    FLOGD("%s()", __func__);
    cmd_t = new std::thread(&SndOutService::handleCmdThread, this);
    SysUtil::setThreadName(cmd_t, "SoundService_cmd");
}

SndOutService::~SndOutService()
{
    {
        std::lock_guard<std::mutex> lock(mlock_stop);
        is_stop = true;
    }
    soundStop();
    {
        std::lock_guard<std::mutex> lock(mlock_cmd);
        mcond_cmd.notify_all();
    }
    cmd_t->join();
    delete cmd_t;
    FLOGD("%s()", __func__);
}

void SndOutService::notify(const char* data, int32_t size)
{   
    std::lock_guard<std::mutex> lock(mlock_stop);
    if (is_stop) return;
    NotifyData* notifyData = (NotifyData*)data;
    if (mTid != notifyData->tid) return;
    switch (notifyData->type) {
    case TYPE_SNDOUT_T_START:
        addCmdData(data, size);
        break;
    case TYPE_SNDOUT_T_STOP:
        addCmdData(data, size);
        break;
    case TYPE_SNDOUT_AAC:
    {
        std::lock_guard<std::mutex> lock(mlock_aac);
        if (aacBuf.size() > TERMINAL_MAX_BUFFER) {
            FLOGD("NOTE::SoundService aacBuf too max, will clean %zu size", aacBuf.size());
        }
        else {
            aacBuf.insert(aacBuf.end(), data, data + size);
            mcond_aac.notify_one();
        }
        break;
    }
    }
}

void SndOutService::notifyPcmData(const char* data, int32_t size, int32_t sample, int32_t channel, int32_t format, int64_t pts)
{
    if (is_sound) {
        char params[28];
        ByteUtil::int64ToBytes(mTid,params, 0, true);
        ByteUtil::int32ToBytes(sample,params, 8, true);
        ByteUtil::int32ToBytes(channel,params, 12, true);
        ByteUtil::int32ToBytes(format,params, 16, true);
        ByteUtil::int64ToBytes(pts,params, 20, true);
        N->handledata(NOTI_SNDOUT_PCM, data, size, params);
    }
}

void SndOutService::addCmdData(const char* data, int32_t size)
{
    std::lock_guard<std::mutex> lock(mlock_cmd);
    if (cmdBuf.size() > TERMINAL_MAX_BUFFER) {
        FLOGD("NOTE::SoundService cmdBuf too max, will clean %zu size", cmdBuf.size());
    }
    else {
        cmdBuf.insert(cmdBuf.end(), data, data + size);
        mcond_cmd.notify_one();
    }
}

void SndOutService::handleCmdThread()
{
    while (!is_stop) {
        char* data = nullptr;
        {
            std::unique_lock<std::mutex> lock(mlock_cmd);
            while (!is_stop && cmdBuf.empty()) {
                mcond_cmd.wait(lock);
            }
            if (is_stop) break;
            int32_t dLen = ByteUtil::getInt32(&cmdBuf[0] + 4);
            int32_t aLen = dLen + 8;
            if (cmdBuf.size() < aLen) {
                FLOGE("SndOutService recv cmd is error!");
                cmdBuf.clear();
                continue;
            }
            data = (char*)malloc(aLen * sizeof(char));
            if (!data) continue;
            memcpy(data, &cmdBuf[0], aLen);
            cmdBuf.erase(cmdBuf.begin(), cmdBuf.begin() + aLen);
        }        
        NotifyData* notifyData = (NotifyData*)data;
        switch (notifyData->type) {
        case TYPE_SNDOUT_T_START:
            soundStop();
            soundStart();
            break;
        case TYPE_SNDOUT_T_STOP:
            soundStop();
            break;
        }
        free(data);
    }
}

void SndOutService::soundStart()
{
    N->lock();
    if (is_sound) {
        N->unlock();
        return;
    }
    is_sound = true;

    mAudioDecoder = new AudioDecoder(this, "[SNDOUT]");
    mAudioDecoder->initCodec("aac", 48000, AV_CH_LAYOUT_STEREO, AV_SAMPLE_FMT_S16);
    aac_t = new std::thread(&SndOutService::handleAacThread, this);
    SysUtil::setThreadName(aac_t, "SoundService_aac");
    N->unlock();
    N->miniNotify((const char*)SNDOUT_U_START, sizeof(SNDOUT_U_START), mTid, 0x12345678);
}

void SndOutService::soundStop()
{
    N->lock();
    if (!is_sound) {
        N->unlock();
        return;
    }
    is_sound = false;
    {
        std::lock_guard<std::mutex> lock(mlock_aac);
        mcond_aac.notify_all();
    }
    if (aac_t) {
        aac_t->join();
        delete aac_t;
        aac_t = nullptr;
    }
    if (mAudioDecoder) {
        mAudioDecoder->releaseCodec();
        delete mAudioDecoder;
        mAudioDecoder = nullptr;
    }
    N->unlock();
}

void SndOutService::handleAacThread()
{
    while (is_sound) {
        std::unique_lock<std::mutex> lock(mlock_aac);
        while (is_sound && aacBuf.empty()) {
            mcond_aac.wait(lock);
        }
        if (!is_sound) break;
        int32_t dLen = ByteUtil::getInt32(&aacBuf[0] + 4);
        int32_t aLen = dLen + 8;
        if (mAudioDecoder) {
            mAudioDecoder->inAacData(&aacBuf[0] + sizeof(SNDOUT_AAC), aLen - sizeof(SNDOUT_AAC), 0);
        }
        aacBuf.erase(aacBuf.begin(), aacBuf.begin() + aLen);
    }
}
