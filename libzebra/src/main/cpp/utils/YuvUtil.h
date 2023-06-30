//
// Created by Administrator on 2022/2/11.
//

#ifndef F_ZEBRA_YUVUTIL_H
#define F_ZEBRA_YUVUTIL_H

#include <stdint.h>

class YuvUtil {
public:
    static void I420Copy(const char *src, char *obj, int32_t width, int32_t height);
};

#endif //F_ZEBRA_YUVUTIL_H
