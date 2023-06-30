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
    static BufferManager* get();
    void regNotify(Notify* notify);
    void unregNotify(Notify* notify);
    ~BufferManager();
    void notify(const char* data, int32_t size) override;
    void handle(NofifyType type, const char* data, int32_t size, const char* params) override;
    LoopBuf* createBuffer(size_t capacity, size_t itemsize, const char* tag);
    void releaseBuffer(LoopBuf* buffer);

private:
    BufferManager();
    void selfFixedThread() const;

private:
    static BufferManager* m_pInstance;
    bool is_stop;
    std::thread* fixed_t;
    std::mutex mlock_list;
    std::list<LoopBuf*> bufferList;
};

#endif //F_ZEBRA_BUFFERMANAGER_H
