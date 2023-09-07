#include <stdint.h>
#include "Base64.h"
#include "utils/FlyLog.h"

char Base64::encrypt6Bit(unsigned x) {
    if (x <= 25) {
        return 'A' + x;
    }
    else if (x <= 51) {
        return 'a' + x - 26;
    }
    else if (x <= 61) {
        return '0' + x - 52;
    }
    else if (x == 62) {
        return '-';
    }
    else {
        return '_';
    }
}

size_t Base64::encrypt(const char* in_data, int32_t in_size, char* out_buffer, int32_t buf_size)
{
    const uint8_t* data = (const uint8_t*)in_data;
    uint8_t* out = (uint8_t*)out_buffer;

    int i = 0;
    int n = 0;
    for (i = 0; i < (in_size / 3) * 3; i += 3) {
        uint8_t x1 = data[i];
        uint8_t x2 = data[i + 1];
        uint8_t x3 = data[i + 2];
        out[n++] = (encrypt6Bit(x1 >> 2));
        out[n++] = (encrypt6Bit((x1 << 4 | x2 >> 4) & 0x3f));
        out[n++] = (encrypt6Bit((x2 << 2 | x3 >> 6) & 0x3f));
        out[n++] = (encrypt6Bit(x3 & 0x3f));
    }
    switch (in_size % 3) {
    case 0:
        break;
    case 2:
    {
        uint8_t x1 = data[i];
        uint8_t x2 = data[i + 1];
        out[n++] = (encrypt6Bit(x1 >> 2));
        out[n++] = (encrypt6Bit((x1 << 4 | x2 >> 4) & 0x3f));
        out[n++] = (encrypt6Bit((x2 << 2) & 0x3f));
        out[n++] = ('=');
        break;
    }
    default:
    {
        uint8_t x1 = data[i];
        out[n++] = (encrypt6Bit(x1 >> 2));
        out[n++] = (encrypt6Bit((x1 << 4) & 0x3f));
        out[n++] = ('=');
        out[n++] = ('=');
        break;
    }
    }
    return n;
}

size_t Base64::decrypt(const char* in_data, int32_t in_size, char* out_buffer, int32_t buf_size)
{
    if ((in_size % 4) != 0) {
        FLOGE("Base64 decrypt data size error.");
        return 0;
    }
    int32_t padding = 0;
    if (in_size >= 1 && in_data[in_size - 1] == '=') {
        padding = 1;

        if (in_size >= 2 && in_data[in_size - 2] == '=') {
            padding = 2;

            if (in_size >= 3 && in_data[in_size - 3] == '=') {
                padding = 3;
            }
        }
    }
    size_t retLen = (in_size / 4) * 3 - padding;
    if (buf_size < retLen) {
        FLOGE("Base64 decrypt buffer size is too small.");
        return 0;
    }
    size_t j = 0;
    uint32_t accum = 0;
    for (size_t i = 0; i < in_size; ++i) {
        char c = in_data[i];
        unsigned value;
        if (c >= 'A' && c <= 'Z') {
            value = c - 'A';
        }
        else if (c >= 'a' && c <= 'z') {
            value = 26 + c - 'a';
        }
        else if (c >= '0' && c <= '9') {
            value = 52 + c - '0';
        }
        else if (c == '+' || c == '-') {
            value = 62;
        }
        else if (c == '/' || c == '_') {
            value = 63;
        }
        else if (c != '=') {
            return 0;
        }
        else {
            if (i < in_size - padding) {
                FLOGE("Base64 decrypt error.");
                return 0;
            }
            value = 0;
        }
        accum = (accum << 6) | value;
        if (((i + 1) % 4) == 0) {
            if (j < retLen) { out_buffer[j++] = (accum >> 16); }
            if (j < retLen) { out_buffer[j++] = (accum >> 8) & 0xff; }
            if (j < retLen) { out_buffer[j++] = accum & 0xff; }
            accum = 0;
        }
    }
    return j;
}
