#include <stdint.h>
#include "Base64.h"

uint8_t Base64::encode6Bit(unsigned x) {
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
        return '+';
    }
    else {
        return '/';
    }
}

void Base64::encode(const uint8_t* _data, int32_t size, uint8_t* text, int32_t* outLen)
{
    const uint8_t* data = (const uint8_t*)_data;
    uint8_t* out = (uint8_t*)text;

    int i = 0;
    int n = 0;
    for (i = 0; i < (size / 3) * 3; i += 3) {
        uint8_t x1 = data[i];
        uint8_t x2 = data[i + 1];
        uint8_t x3 = data[i + 2];

        out[n++] = (encode6Bit(x1 >> 2));
        out[n++] = (encode6Bit((x1 << 4 | x2 >> 4) & 0x3f));
        out[n++] = (encode6Bit((x2 << 2 | x3 >> 6) & 0x3f));
        out[n++] = (encode6Bit(x3 & 0x3f));
    }
    switch (size % 3) {
    case 0:
        break;
    case 2:
    {
        uint8_t x1 = data[i];
        uint8_t x2 = data[i + 1];
        out[n++] = (encode6Bit(x1 >> 2));
        out[n++] = (encode6Bit((x1 << 4 | x2 >> 4) & 0x3f));
        out[n++] = (encode6Bit((x2 << 2) & 0x3f));
        out[n++] = ('=');
        break;
    }
    default:
    {
        uint8_t x1 = data[i];
        out[n++] = (encode6Bit(x1 >> 2));
        out[n++] = (encode6Bit((x1 << 4) & 0x3f));
        out[n++] = ('=');
        out[n++] = ('=');
        break;
    }
    }
    *outLen = n;
}

void Base64::decode(const uint8_t* _text, int32_t size, uint8_t* data, int32_t* outLen)
{
    if ((size % 4) != 0) {
        *outLen = -1;
        return;
    }
    int32_t padding = 0;
    if (size >= 1 && _text[size - 1] == '=') {
        padding = 1;

        if (size >= 2 && _text[size - 2] == '=') {
            padding = 2;

            if (size >= 3 && _text[size - 3] == '=') {
                padding = 3;
            }
        }
    }
    size_t retLen = (size / 4) * 3 - padding;
    if (*outLen < retLen) {
        *outLen = -1;
        return;
    }
    size_t j = 0;
    uint32_t accum = 0;
    for (size_t i = 0; i < size; ++i) {
        uint8_t c = _text[i];
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
            *outLen = -1;
            return;
        }
        else {
            if (i < size - padding) {
                *outLen = -1;
                return;
            }

            value = 0;
        }
        accum = (accum << 6) | value;
        if (((i + 1) % 4) == 0) {
            if (j < retLen) { data[j++] = (accum >> 16); }
            if (j < retLen) { data[j++] = (accum >> 8) & 0xff; }
            if (j < retLen) { data[j++] = accum & 0xff; }
            accum = 0;
        }
    }
    *outLen = j;
}
