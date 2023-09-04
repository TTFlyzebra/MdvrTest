//
// Created by Administrator on 2022/2/12.
//

#ifndef F_ZEBRA_BASENOTIFY_H
#define F_ZEBRA_BASENOTIFY_H

#include "Notify.h"

class BaseNotify : public INotify {
public:
    BaseNotify(Notify* notify);
    ~BaseNotify();
    void notify(const char* data, int32_t size) override;
    void handle(NofifyType type, const char* data, int32_t size, const char* params) override;

public:
    Notify* N;
    volatile bool is_stop;
    std::mutex mlock_stop;
};



#endif //F_ZEBRA_BASENOTIFY_H
