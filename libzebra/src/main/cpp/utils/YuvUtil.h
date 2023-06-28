//
// Created by Administrator on 2022/2/11.
//

#ifndef F_ZEBRA_YUVUTIL_H
#define F_ZEBRA_YUVUTIL_H

#include <stdint.h>
#include <libyuvz.h>

class YuvUtil {
public:
    static void NV21ToNV12(const char* src, char* obj, int32_t width, int32_t height);

    static void NV12Scale(const char* src, char* obj, int32_t w1, int32_t h1, int32_t w2, int32_t h2);

    static void NV21ToI420(const char* src, char* obj, int32_t width, int32_t height);

    static void NV12ToI420(const char* src, char* obj, int32_t width, int32_t height);

    static void I420Copy(const char* src, char* obj, int32_t width, int32_t height);

    static void I420Scale(const char* src, char* obj, int32_t w1, int32_t h1, int32_t w2, int32_t h2);

    static void I420ToNV21(const char* src, char* obj, int32_t width, int32_t height);

    static void I420ToNV12(const char* src, char* obj, int32_t width, int32_t height);

    static void ARGBToNV12(const char* src, char* obj, int32_t width, int32_t height);

    static void ConvertToI420(const char* src, char* obj, int32_t w1, int32_t h1, int32_t w2, int32_t h2, uint32_t fourcc);

};

#endif //F_ZEBRA_YUVUTIL_H
