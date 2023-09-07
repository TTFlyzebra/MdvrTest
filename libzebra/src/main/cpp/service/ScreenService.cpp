#include "ScreenService.h"

#include "Config.h"
#include "utils/FlyLog.h"
#include "rfc/Protocol.h"
#include "utils/ByteUtil.h"
#include "utils/SysUtil.h"
#include "media/VideoDecoder.h"
#include "base/Global.h"

ScreenService::ScreenService(Notify* notify, int64_t tid)
    : BaseNotify(notify)
    , mTid(tid)
    , mVideoDecoder(nullptr)
    , avc_t(nullptr)
    , is_screen(false)
{
    char sn[16] = { 0 };
    ByteUtil::int64ToSysId(sn, mTid);
    FLOGD("[%s]%s()", sn, __func__);
    {
        N->registerListener(this);
        std::lock_guard<std::mutex> lock_stop(mlock_stop);
        is_stop = false;        
    }
    cmd_t = new std::thread(&ScreenService::handleCmdThread, this);
    SysUtil::setThreadName(cmd_t, "ScreenService_cmd");
}

ScreenService::~ScreenService()
{
    {
        N->unregisterListener(this);
        std::lock_guard<std::mutex> lock_stop(mlock_stop);
        is_stop = true;        
    }
    screenStop();
    {
        std::lock_guard<std::mutex> lock_cmd(mlock_cmd);
        mcond_cmd.notify_all();
    }
    cmd_t->join();
    delete cmd_t;
    char sn[16] = { 0 };
    ByteUtil::int64ToSysId(sn, mTid);
    FLOGD("[%s]%s()", sn, __func__);
}

void ScreenService::notify(const char* data, int32_t size)
{
    std::lock_guard<std::mutex> lock_stop(mlock_stop);
    if (is_stop) return;
    NotifyData* notifyData = (NotifyData*)data;
    if (mTid != notifyData->tid ) return;
    switch (notifyData->type) {
    case TYPE_SCREEN_T_START:
        addCmdData(data, size);
        break;
    case TYPE_SCREEN_T_STOP:
        addCmdData(data, size);
        break;
    case TYPE_SCREEN_AVC:
        std::lock_guard<std::mutex> lock_avc(mlock_avc);
        if (avcBuf.size() > MAX_SOCKET_BUFFER) {
            FLOGD("NOTE::ScreenService avcBuf too max, will clean %zu size", avcBuf.size());
        }
        else {
            avcBuf.insert(avcBuf.end(), data, data + size);
            mcond_avc.notify_one();
        }
        break;
    }
}

void ScreenService::notifyYuvData(const char* data, int32_t size, int32_t width, int32_t height, int64_t pts)
{
    if (is_screen) {
        char params[24];
        ByteUtil::int64ToBytes(mTid,params, 0, true);
        ByteUtil::int32ToBytes(width,params, 8, true);
        ByteUtil::int32ToBytes(height,params, 12, true);
        ByteUtil::int64ToBytes(pts,params, 16, true);
        N->handledata(NOTI_SCREEN_YUV, data, size, params);
    }
}

void ScreenService::addCmdData(const char* data, int32_t size)
{
    std::lock_guard<std::mutex> lock_cmd(mlock_cmd);
    if (cmdBuf.size() > MAX_CMD_BUFFER) {
        FLOGD("NOTE::ScreenService cmdBuf too max, will clean %zu size", cmdBuf.size());
    }
    else {
        cmdBuf.insert(cmdBuf.end(), data, data + size);
        mcond_cmd.notify_one();
    }
}

void ScreenService::handleCmdThread()
{
    while (!is_stop) {
        char* data = nullptr;
        {
            std::unique_lock<std::mutex> lock_cmd(mlock_cmd);
            while (!is_stop && cmdBuf.empty()) {
                mcond_cmd.wait(lock_cmd);
            }
            if (is_stop) break;
            int32_t dLen = ByteUtil::getInt32(&cmdBuf[0] + 4);
            int32_t aLen = dLen + 8;
            if (cmdBuf.size() < aLen) {
                FLOGE("ScreenService recv cmd is error!");
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
        case TYPE_SCREEN_T_START:
        {
            uint16_t width = ByteUtil::getInt16(data + 16);
            uint16_t height = ByteUtil::getInt16(data + 18);
            uint16_t format = ByteUtil::getInt16(data + 20);
            screenStop();
            screenStart(width, height, format);
            break;
        }
        case TYPE_SCREEN_T_STOP:
            screenStop();
            break;
        }
        free(data);
    }
}


void ScreenService::screenStart(uint16_t width, uint16_t height, uint16_t format)
{
    N->lock();
    if (is_screen) {
        N->unlock();
        return;
    }
    is_screen = true;
    char sn[16] = { 0 };
    ByteUtil::int64ToSysId(sn, mTid);
    mVideoDecoder = new VideoDecoder(this, sn);
    mVideoDecoder->initCodec(format==1?"hevc":"h264", width, height);
    avc_t = new std::thread(&ScreenService::handleAvcThread, this);
    SysUtil::setThreadName(avc_t, "ScreenService_avc");
    N->unlock();
    N->miniNotify(
        (const char*)SCREEN_U_START, 
        sizeof(SCREEN_U_START), 
        mTid,
        U->uid
    );
}

void ScreenService::screenStop()
{

    N->lock();
    if (!is_screen) {
        N->unlock();
        return;
    }
    is_screen = false;
    if (mVideoDecoder) {
        delete mVideoDecoder;
        mVideoDecoder = nullptr;
    }
    {
        std::lock_guard<std::mutex> lock_avc(mlock_avc);
        mcond_avc.notify_all();
    }
    if (avc_t) {
        avc_t->join();
        delete avc_t;
        avc_t = nullptr;
    }
    N->unlock();

}

void ScreenService::handleAvcThread()
{
    while (is_screen) {
        std::unique_lock<std::mutex> lock_avc(mlock_avc);
        while (is_screen && avcBuf.empty()) {
            mcond_avc.wait(lock_avc);
        }
        if (!is_screen) break;
        int32_t dLen = (avcBuf[4] & 0xFF) << 24 | (avcBuf[5] & 0xFF) << 16 | (avcBuf[6] & 0xFF) << 8 | (avcBuf[7] & 0xFF);
        int32_t aLen = dLen + 8;
        if (mVideoDecoder) {
            mVideoDecoder->inAvcData(&avcBuf[0] + sizeof(SCREEN_AVC), aLen - sizeof(SCREEN_AVC));
        }
        avcBuf.erase(avcBuf.begin(), avcBuf.begin() + aLen);
    }
}
