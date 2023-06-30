//
// Created by Administrator on 2022/4/25.
//

#include <unistd.h>
#include "BufferManager.h"
#include "LoopBuf.h"
#include "utils/FlyLog.h"

BufferManager* BufferManager::m_pInstance = nullptr;

void BufferManager::init()
{
    m_pInstance = new BufferManager();
}

void BufferManager::release()
{
    delete m_pInstance;
}

BufferManager* BufferManager::instance()
{
    return m_pInstance;
}

BufferManager::BufferManager()
    : is_stop(false)
    , fixed_t(nullptr)
{
    FLOGD("%s()", __func__);
    fixed_t = new std::thread(&BufferManager::selfFixedThread, this);
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

void BufferManager::handle(int32_t type, const char* data, int32_t size, int32_t p1, int32_t p2, int32_t p3, int64_t p4, int64_t tid)
{

}

LoopBuf* BufferManager::createBuffer(size_t capacity, size_t itemsize, const char* tag)
{
    LoopBuf* buffer = new LoopBuf(capacity, itemsize, tag);
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

void BufferManager::selfFixedThread()
{
    int32_t test_count = 0;
    while (!is_stop) {
        for (int i = 0; i < 10; i++) {
            usleep(100000);
            if (is_stop) return;
        }   
    }
}