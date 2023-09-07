#ifndef ANDROID_SNDOUTSERVICE_H
#define ANDROID_SNDOUTSERVICE_H

#include "base/BaseNotify.h"
#include "media/AudioDecoderCB.h"

class Terminal;
class AudioDecoder;

class SndOutService
    : public BaseNotify
    , public AudioDecoderCB
{
public:
    SndOutService(Notify* notify, int64_t tid);
    ~SndOutService();
    void notify(const char* data, int32_t size) override;
    void notifyPcmData(const char* data, int32_t size, int32_t sample, int32_t channel, int32_t format, int64_t pts) override;

private:
    void addCmdData(const char* data, int32_t size); 
    void handleCmdThread();                          
    void soundStart();
    void soundStop();
    void handleAacThread();

private:
    int64_t mTid;
    AudioDecoder* mAudioDecoder;

    std::thread* cmd_t;
    std::mutex mlock_cmd;

    std::vector<char> cmdBuf;
    std::condition_variable mcond_cmd;

    std::thread* aac_t;
    std::mutex mlock_aac;
    std::vector<char> aacBuf;
    std::condition_variable mcond_aac;

    bool is_sound;
};

#endif //ANDROID_SNDOUTSERVICE_H
