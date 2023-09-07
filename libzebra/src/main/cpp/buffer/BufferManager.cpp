//
// Created by Administrator on 2022/4/25.
//

#include "BufferManager.h"
#include "LoopBuf.h"
#include "utils/FlyLog.h"
#include "utils/SysUtil.h"
#if defined(WIN32)
#elif defined(__unix)
#include <unistd.h>
#endif

BufferManager* BufferManager::m_pInstance = nullptr;

void BufferManager::init()
{
    m_pInstance = new BufferManager();
}

void BufferManager::release()
{
    delete m_pInstance;
}

BufferManager* BufferManager::get()
{
    return m_pInstance;
}

BufferManager::BufferManager()
        : is_stop(false)
        , fixed_t(nullptr)
{
    FLOGD("%s()", __func__);
    fixed_t = new std::thread(&BufferManager::selfFixedThread, this);
    SysUtil::setThreadName(fixed_t, "BufferManager");
}

BufferManager::~BufferManager()
{
    FLOGD("%s()", __func__);
    is_stop = true;
    fixed_t->join();
    delete fixed_t;
}

void BufferManager::regNotify(Notify* notify)
{
    notify->registerListener(this);
}

void BufferManager::unregNotify(Notify* notify)
{
    notify->unregisterListener(this);
}

void BufferManager::notify(const char* data, int32_t size)
{

}

void BufferManager::handle(NofifyType type, const char* data, int32_t dsize, const char* params, int32_t psize)
{

}

LoopBuf* BufferManager::createBuffer(size_t capacity, size_t itemsize, const char* tag)
{
    auto* buffer = new LoopBuf(capacity, itemsize, tag);
    {
        std::lock_guard<std::mutex> lock(mlock_list);
        bufferList.push_back(buffer);
    }
    return buffer;
}

void BufferManager::releaseBuffer(LoopBuf* buffer)
{
    {
        std::lock_guard<std::mutex> lock(mlock_list);
        bufferList.remove(buffer);
    }
    delete buffer;
}

void BufferManager::selfFixedThread() const
{
    //TODO::
    while (!is_stop) {
        for (int i = 0; i < 10; i++) {
#if defined(WIN32)
            _sleep(100);
#elif defined(__unix)
            usleep(100000);
#endif
            if (is_stop) return;
        }
    }
}