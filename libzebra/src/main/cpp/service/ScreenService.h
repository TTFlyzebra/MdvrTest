#ifndef ANDROID_SCREENSERVICE_H
#define ANDROID_SCREENSERVICE_H

#include "base/BaseNotify.h"
#include "media/VideoDecoderCB.h"
#include "base/User.h"
#include "base/Terminal.h"

class Terminal;
class VideoDecoder;

class ScreenService :
    public BaseNotify,
    public VideoDecoderCB
{

public:
    ScreenService(Notify* notify, int64_t tid);
    ~ScreenService();
    void notify(const char* data, int32_t size) override;
    void notifyYuvData(const char* data, int32_t size, int32_t width, int32_t height, int64_t pts) override;

private:
    void addCmdData(const char* data, int32_t size);
    void handleCmdThread();
    void screenStart(uint16_t width, uint16_t height, uint16_t format);
    void screenStop();
    void handleAvcThread();


private:
    int64_t mTid;
    VideoDecoder* mVideoDecoder;

    std::thread* cmd_t;
    std::mutex mlock_cmd;
    std::vector<char> cmdBuf;
    std::condition_variable mcond_cmd;

    std::thread* avc_t;
    std::mutex mlock_avc;
    std::vector<char> avcBuf;
    std::condition_variable mcond_avc;

    bool is_screen;
};

#endif //ANDROID_SCREENSERVICE_H
