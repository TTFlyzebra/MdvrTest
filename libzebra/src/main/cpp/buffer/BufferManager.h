//
// Created by Administrator on 2022/4/25.
//

#ifndef F_ZEBRA_BUFFERMANAGER_H
#define F_ZEBRA_BUFFERMANAGER_H

#include "base/Notify.h"
#include <map>

class LoopBuf;

class BufferManager : public INotify{
public:
    static void init();
    static void release();
    static BufferManager* instance();
    void regNotify(Notify* notify);
    void unregNotify(Notify* notify);
    ~BufferManager();
    virtual void notify(const char* data, int32_t size) override;
    virtual void handle(int32_t type, const char* data, int32_t size, int32_t p1, int32_t p2, int32_t p3, int64_t p4, int64_t tid) override;
    LoopBuf* createBuffer(size_t capacity, size_t itemsize, const char* tag);
    void releaseBuffer(LoopBuf* buffer);

private:
    BufferManager();
    void selfFixedThread();

private:
    static BufferManager* m_pInstance;
    bool is_stop;
    std::thread* fixed_t;
    std::mutex mlock_list;
    std::list<LoopBuf*> bufferList;
};

#endif //F_ZEBRA_BUFFERMANAGER_H
