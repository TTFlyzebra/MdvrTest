//
// Created by FlyZebra on 2021/9/15 0015.
//

#include <unistd.h>
#include "Notify.h"

#include "Config.h"
#include "buffer/BufferManager.h"
#include "buffer/LoopBuf.h"
#include "utils/FlyLog.h"
#include "utils/ByteUtil.h"
#include "utils/SysUtil.h"

Notify::Notify()
    : is_stop(false)
    , mlist_count(0)
{
    FLOGD("%s()", __func__);
    dataBuf = BufferManager::get()->createBuffer(1024*1024*10, 1024, "Notify");
    data_t = new std::thread(&Notify::updataThread, this);
    SysUtil::setThreadName(data_t, "Notify_data");
}

Notify::~Notify()
{
    is_stop = true;
    {
        std::lock_guard<std::mutex> lock(mlock_data);
        mcond_data.notify_all();
    }
    {
        std::lock_guard<std::mutex> lock(mlock_list);
        notifyList.clear();
    }
    data_t->join();
    delete data_t;
    BufferManager::get()->releaseBuffer(dataBuf);
    FLOGD("%s()", __func__);
}

void Notify::registerListener(INotify *notify)
{
    if (is_stop) return;
    while (mlist_count > 0) {
        usleep(100000);
    }
    std::lock_guard<std::mutex> lock(mlock_list);
    notifyList.push_back(notify);
}

void Notify::unregisterListener(INotify *notify)
{
    if (is_stop) return;
    while (mlist_count > 0) {
        usleep(100000);
    }
    std::lock_guard<std::mutex> lock(mlock_list);
    notifyList.remove(notify);
}

void Notify::notifydata(const char *data, int32_t size)
{
    mlist_count++;
    for (auto &it: notifyList) {
        ((INotify*)it)->notify(data, size);
    }
    mlist_count--;
}

void Notify::handledata(NofifyType type, const char* data, int32_t size, const char* params)
{
    mlist_count++;
    for (auto &it: notifyList) {
        ((INotify*)it)->handle(type, data, size, params);
    }
    mlist_count--;
}

void Notify::miniNotify(const char* command, int32_t size, int64_t tid, int64_t uid, const char* data)
{
    char* sendcmd = (char*)malloc(size * sizeof(char));
    memcpy(sendcmd, command, size);
    int32_t start = 8;
    if (tid != 0) {
        memcpy(sendcmd + start, &tid, 8);
        start += 8;
    }
    if (uid != 0) {
        memcpy(sendcmd + start, &uid, 8);
        start += 8;
    }
    if (data) {
        memcpy(sendcmd + start, data, size - start);
    }
    {
        std::lock_guard<std::mutex> lock(mlock_data);
        size_t ret = dataBuf->push(sendcmd, size);
        if (!ret) {
            FLOGE("NOTE::Notify upCommand dataBuf too max, [%zu]", dataBuf->size());
        }
        mcond_data.notify_one();
    }
    free(sendcmd);
}


void Notify::fullNotify(const char* data, int32_t size)
{
    std::lock_guard<std::mutex> lock(mlock_data);
    size_t ret = dataBuf->push(data, size);
    if (!ret) {
        FLOGE("NOTE::Notify upCommand dataBuf too max, [%zu]", dataBuf->size());
    }
    mcond_data.notify_one();
}

void Notify::lock()
{
    mlock_all.lock();
}

void Notify::unlock()
{
    mlock_all.unlock();
}

bool Notify::try_lock()
{
    bool flag = mlock_all.try_lock();
    if(flag) mlock_all.unlock();
    return flag;
}

bool Notify::tryLock(std::mutex* lock)
{
    bool flag = lock->try_lock();
    if(flag) lock->unlock();
    return flag;
}

void Notify::loghex(const char* data, int32_t size, const char* tag, int32_t max)
{
    int32_t num = size < max ? size : max;
    memset(logdata, 0, sizeof(logdata));
    for (int32_t i = 0; i < num; i++) {
        sprintf(logdata, "%s%02x:", logdata, data[i] & 0xFF);
    }
    FLOGE("%s->%s[%d]", tag, logdata, size);
}

void Notify::updataThread()
{
    while (!is_stop) {
        char* data = nullptr;
        int32_t dLen = 0;
        int32_t aLen = 0;
        {
            std::unique_lock<std::mutex> lock(mlock_data);
            while (!is_stop && dataBuf->size() < 8) {
                mcond_data.wait(lock);
            }
            if (is_stop) break;
            data = dataBuf->popTemp(8);
            if (((data[0] & 0xFF) != 0xEE) || ((data[1] & 0xFF) != 0xAA)) {
                FLOGE("Notify updataThread header error.[%02x:%02x]size[%d]", data[0] & 0xFF, data[1] & 0xFF, dataBuf->size());
                loghex(data, 8);
                dataBuf->clear();
                continue;
            }
            dLen = ByteUtil::getInt32(data + 4);
            aLen = dLen + 8;
            //while (!is_stop && (aLen > dataBuf->size())) {
            //    mcond_data.wait(lock);
            //}
            //if (is_stop) break;
            data = dataBuf->pop(aLen);
        }
        notifydata(data, aLen);
    }
}
