//
// Created by Administrator on 2022/2/11.
//

#ifndef F_ZEBRA_BASE64_H
#define F_ZEBRA_BASE64_H

#include <stdint.h>

class Base64 {
public:
    Base64() {};
    ~Base64() {};
    static size_t encrypt(const char* in_data, int32_t in_size, char* out_buffer, int32_t buf_size);
    static size_t decrypt(const char* in_data, int32_t in_size, char* out_buffer, int32_t buf_size);
private:
    static char encrypt6Bit(unsigned x);
};



#endif //F_ZEBRA_BASE64_H
