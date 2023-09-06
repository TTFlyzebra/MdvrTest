//
// Created by Administrator on 2022/2/11.
//

#include "YuvUtil.h"
#include <libyuv.h>

void YuvUtil::I420Copy(const char* src, char* obj, int32_t width, int32_t height)
{
    libyuv::I420Copy(
        (const uint8_t*)src,
        width,
        (const uint8_t*)src+ width * height,
        width / 2,
        (const uint8_t*)src+ width * height * 5 / 4,
        width / 2,
        (uint8_t*)obj,
        width,
        (uint8_t*)obj + width * height,
        width / 2,
        (uint8_t*)obj + width * height * 5 / 4,
        width / 2,
        width,
        height);
}
