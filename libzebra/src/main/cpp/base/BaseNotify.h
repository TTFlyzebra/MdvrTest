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
    virtual void notify(const char* data, int32_t size) override;
    virtual void handle(int32_t type, const char* data, int32_t size, int32_t p1, int32_t p2, int32_t p3, int64_t p4, int64_t tid) override;

public:
    Notify* N;
    volatile bool is_stop;
};



#endif //F_ZEBRA_BASENOTIFY_H
