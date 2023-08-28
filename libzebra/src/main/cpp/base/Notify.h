//
// Created by FlyZebra on 2021/9/15 0015.
//

#ifndef F_ZEBRA_NOTIFY_H
#define F_ZEBRA_NOTIFY_H

#include <stdint.h>
#include <list>
#include <vector>
#include <mutex>
#include <thread>
#include <atomic>
#include <condition_variable>

class LoopBuf;

struct NotifyData {
    int16_t head;
    int16_t type;
    int32_t size;
    int64_t tid;
    int64_t uid;
    char *data;
};

enum NofifyType {
    NOTI_COMMAND = 0,
    NOTI_SCREEN_YUV,
    NOTI_SCREEN_AVC,
    NOTI_SNDOUT_PCM,
    NOTI_SNDOUT_AAC,
    NOTI_CAMOUT_YUV,
    NOTI_CAMOUT_AVC,
    NOTI_CAMFIX_YUV,
    NOTI_CAMFIX_AVC,
    NOTI_MICOUT_PCM,
    NOTI_MICOUT_AAC,
    NOTI_MICFIX_PCM,
    NOTI_MICFIX_AAC,
    NOTI_SCREEN_SPS,
    NOTI_SNDOUT_SPS,
    NOTI_CAMOUT_SPS,
    NOTI_MICOUT_SPS,
    NOTI_CAMSUB_YUV,
};

class INotify {
public:
    virtual ~INotify() {};

    virtual void notify(const char *data, int32_t size) = 0;

    virtual void handle(NofifyType type, const char *data, int32_t size, const char *params) = 0;
};

class Notify {
public:
    Notify();

    ~Notify();

    void registerListener(INotify *notify);

    void unregisterListener(INotify *notify);

    void notifydata(const char *data, int32_t size);

    void handledata(NofifyType type, const char *data, int32_t size, const char *params);

    void miniNotify(const char *command, int32_t size, int64_t tid, int64_t uid = 0, const char *data = nullptr);

    void fullNotify(const char *data, int32_t size);

    void lock();

    void unlock();

    bool try_lock();

    bool tryLock(std::mutex *lock);

    void loghex(const char *data, int32_t size, const char *tag = "Loghex", int32_t max = 32);

private:
    void updataThread();

private:
    volatile bool is_stop;

    std::mutex mlock_list;
    std::atomic<int32_t> mlist_count;
    std::list<INotify *> notifyList;

    std::thread *data_t;
    std::mutex mlock_data;
    LoopBuf *dataBuf;
    std::condition_variable mcond_data;

    std::mutex mlock_all;

    char logdata[1024]{};
};

#endif //F_ZEBRA_NOTIFY_H
