//
// Created by Administrator on 2022/2/12.
//

#include "BaseNotify.h"

BaseNotify::BaseNotify(Notify* notify)
: N(notify)
, is_stop(false)
{
    N->registerListener(this);
}
BaseNotify::~BaseNotify()
{
    is_stop = true;
    N->unregisterListener(this);
}

void BaseNotify::notify(const char* data, int32_t size)
{

}

void BaseNotify::handle(int32_t type, const char* data, int32_t size, int32_t p1, int32_t p2, int32_t p3, int64_t p4, int64_t tid)
{

}

