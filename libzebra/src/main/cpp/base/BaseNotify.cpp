//
// Created by Administrator on 2022/2/12.
//

#include "BaseNotify.h"

BaseNotify::BaseNotify(Notify* notify)
    : N(notify)
{

}

BaseNotify::~BaseNotify()
{

}

void BaseNotify::notify(const char* data, int32_t size)
{

}

void BaseNotify::handle(NofifyType type, const char* data, int32_t size, const char* params)
{

}

