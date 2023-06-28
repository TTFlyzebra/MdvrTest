//
// Created by Administrator on 2022/2/12.
//

#include "ByteUtil.h"
#include <string.h>

int16_t ByteUtil::getInt16(const char* data)
{
    return (int16_t)(data[0] & 0xFF) << 8 | (data[1] & 0xFF);
}

int32_t ByteUtil::getInt32(const char* data)
{
    return (int32_t)(data[0] & 0xFF) << 24 | (data[1] & 0xFF) << 16 | (data[2] & 0xFF) << 8 | (data[3] & 0xFF);
}

void ByteUtil::int16ToData(char* data, int16_t i1, int16_t i2, int16_t i3, int16_t i4)
{
    data[0] = i1 >> 8 & 0xFF;
    data[1] = i1 & 0xFF;
    if (i2 == 0) return;
    data[2] = i2 >> 8 & 0xFF;
    data[3] = i2 & 0xFF;
    if (i3 == 0) return;
    data[4] = i3 >> 8 & 0xFF;
    data[5] = i3 & 0xFF;
    if (i4 == 0) return;
    data[6] = i4 >> 8 & 0xFF;
    data[7] = i4 & 0xFF;
}

void ByteUtil::int32ToData(char* data, int32_t i1, int32_t i2)
{
    data[0] = i1 >> 24 & 0xFF;
    data[1] = i1 >> 16 & 0xFF;
    data[2] = i1 >> 8 & 0xFF;
    data[3] = i1 & 0xFF;
    if (i2 == 0) return;
    data[4] = i2 >> 24 & 0xFF;
    data[5] = i2 >> 16 & 0xFF;
    data[6] = i2 >> 8 & 0xFF;
    data[7] = i2 & 0xFF;
}

int64_t ByteUtil::sysIdToInt64(char* data)
{
    int64_t tid = 0;
    for (int i = 0; i < 9; i++) {
        int64_t ret = 0;
        char x = data[i] & 0xFF;
        if (x >= '0' && x <= '9') {
            ret = x - '0';
        }
        else if (x >= 'A' && x <= 'Z') {
            ret = x - 'A' + 10;
        }
        else if (x >= 'a' && x <= 'z') {
            ret = x - 'a' + 10;
        }
        else {
            continue;
        }
        int64_t pow = 1;
        int32_t y = 9 - i;
        for (int i = 1; i < y; i++) {
            pow = pow * 36;
        }
        tid += ret * pow;
    }
    return tid;
}

void ByteUtil::int64ToSysId(char* data, int64_t tid)
{
    int64_t num = tid;
    for (int i = 0; i < 9; i++) {
        int8_t ni = (num % 36) & 0xFF;
        if (ni < 10) {
            data[8 - i] = ni + '0';
        }
        else {
            data[8 - i] = ni - 10 + 'A';
        }
        num = num / 36;
    }
}