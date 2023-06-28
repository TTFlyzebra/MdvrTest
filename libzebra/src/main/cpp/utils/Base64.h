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
    static void encode(const uint8_t* _data, int32_t size, uint8_t* text, int32_t* outLen);
    static void decode(const uint8_t* _text, int32_t size, uint8_t* data, int32_t* outLen);
private:
    static uint8_t encode6Bit(unsigned x);
};



#endif //F_ZEBRA_BASE64_H
