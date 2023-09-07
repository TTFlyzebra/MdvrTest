//
//Created by Administrator on 2022/2/11.
//

#include "YuvUtil.h"
#include <libyuv.h>
#include <string.h>

void YuvUtil::NV21ToNV12(const char* src, char* obj, int32_t width, int32_t height)
{
    libyuv::NV21ToNV12(
        (const uint8_t*)src,
        width,
        (const uint8_t*)src + width * height,
        width,
        (uint8_t*)obj,
        width,
        (uint8_t*)obj + width * height,
        width,
        width,
        height);
}

void YuvUtil::NV12Scale(const char* src, char* obj, int32_t w1, int32_t h1, int32_t w2, int32_t h2)
{
    libyuv::NV12Scale(
        (const uint8_t*)src,
        w1,
        (const uint8_t*)src + w1 * h1,
        w1,
        w1,
        h1,
        (uint8_t*)obj,
        w2,
        (uint8_t*)obj + w2 * h2,
        w2,
        w2,
        h2,
        libyuv::kFilterNone);
}

void YuvUtil::NV21ToI420(const char* src, char* obj, int32_t width, int32_t height)
{
    libyuv::NV21ToI420(
        (const uint8_t*)src,
        width,
        (const uint8_t*)src + width * height,
        width,
        (uint8_t*)obj,
        width,
        (uint8_t*)obj + width * height,
        width / 2,
        (uint8_t*)obj + width * height * 5 / 4,
        width / 2,
        width,
        height);
}

void YuvUtil::I420Copy(const char* src, char* obj, int32_t width, int32_t height)
{
    libyuv::I420Copy(
        (const uint8_t*)src,
        width,
        (const uint8_t*)src + width * height,
        width / 2,
        (const uint8_t*)src + width * height * 5 / 4,
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

void YuvUtil::I420Scale(const char* src, char* obj, int32_t w1, int32_t h1, int32_t w2, int32_t h2)
{
    libyuv::I420Scale(
        (const uint8_t*)src,
        w1,
        (const uint8_t*)src + w1 * h1,
        w1 / 2,
        (const uint8_t*)src + w1 * h1 * 5 / 4,
        w1 / 2,
        w1,
        h1,
        (uint8_t*)obj,
        w2,
        (uint8_t*)obj + w2 * h2,
        w2 / 2,
        (uint8_t*)obj + w2 * h2 * 5 / 4,
        w2 / 2,
        w2,
        h2,
        libyuv::kFilterNone);
}

void YuvUtil::I420ToNV21(const char* src, char* obj, int32_t width, int32_t height) {
    libyuv::I420ToNV21(
        (const uint8_t*)src,
        width,
        (const uint8_t*)src + width * height,
        width / 2,
        (const uint8_t*)src + width * height * 5 / 4,
        width / 2,
        (uint8_t*)obj,
        width,
        (uint8_t*)obj + width * height,
        width,
        width,
        height);
}

void YuvUtil::I420ToNV12(const char* src, char* obj, int32_t width, int32_t height) {
    libyuv::I420ToNV12(
        (const uint8_t*)src,
        width,
        (const uint8_t*)src + width * height,
        width / 2,
        (const uint8_t*)src + width * height * 5 / 4,
        width / 2,
        (uint8_t*)obj,
        width,
        (uint8_t*)obj + width * height,
        width,
        width,
        height);
}

void YuvUtil::I420Mirror(const char* src, char* obj, int32_t width, int32_t height)
{
    libyuv::I420Mirror(
        (const uint8_t*)src,
        width,
        (const uint8_t*)src + width * height,
        width / 2,
        (const uint8_t*)src + width * height * 5 / 4,
        width / 2,
        (uint8_t*)obj,
        width,
        (uint8_t*)obj + width * height,
        width / 2,
        (uint8_t*)obj + width * height * 5 / 4,
        width / 2,
        width,
        height
    );
}

void YuvUtil::I420MirrorVertical(const char* src, char* obj, int32_t width, int32_t height)
{
    int32_t count = 0;
    char* o1 = obj;
    const char* s1 = src + width * height - width;    
    for (int i = 0; i < height; i++) {
        memcpy(o1 + count, s1 - count, width);
        count += width;
    }
    count = 0;
    char* o2 = obj + width * height;
    const char* s2 = src + width * height * 5 / 4 - width / 2;
    char* o3 = obj + width * height * 5 / 4;
    const char* s3 = src + width * height * 3 / 2 - width / 2;   
    for (int i = 0; i < height / 2; i++) {
        memcpy(o2 + count, s2 - count, width / 2);
        memcpy(o3 + count, s3 - count, width / 2);
        count += width / 2;
    }
}

void YuvUtil::ARGBToNV12(const char* src, char* obj, int32_t width, int32_t height) {
    libyuv::ARGBToNV12(
        (const uint8_t*)src,
        width * height,
        (uint8_t*)obj,
        width,
        (uint8_t*)obj + width * height,
        width,
        width,
        height
    );
}

void YuvUtil::ConvertToI420(const char* src, char* obj, int32_t w1, int32_t h1, int32_t w2, int32_t h2, uint32_t fourcc) {
    libyuv::ConvertToI420(
        (const uint8_t*)src,
        0,
        (uint8_t*)obj,
        w2,
        (uint8_t*)obj + w2 * h2,
        w2 / 2,
        (uint8_t*)obj + w2 * h2 * 5 / 4,
        w2 / 2,
        0,
        0,
        w1,
        h1,
        w2,
        h2,
        libyuv::kRotate0,
        fourcc);
}

void YuvUtil::BGRAToI420(const char* src, char* obj, int32_t w1, int32_t h1, int32_t w2, int32_t h2)
{
    libyuv::BGRAToI420(
        (const uint8_t*)src,
        w1,
        (uint8_t*)obj,
        w2,
        (uint8_t*)obj + w2 * h2,
        w2 / 2,
        (uint8_t*)obj + w2 * h2 * 5 / 4,
        w2 / 2,
        w2,
        h2
    );
}