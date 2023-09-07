//
// Created by Administrator on 2022/2/12.
//

#include "ByteUtil.h"
#include <cstring>

int64_t ByteUtil::sysIdToInt64(char *data, int len) {
    int64_t tid = 0;
    for (int i = 0; i < len; i++) {
        int64_t ret = 0;
        char x = data[i] & 0xFF;
        if (x >= '0' && x <= '9') {
            ret = x - '0';
        } else if (x >= 'A' && x <= 'Z') {
            ret = x - 'A' + 10;
        } else if (x >= 'a' && x <= 'z') {
            ret = x - 'a' + 10;
        } else {
            continue;
        }
        int64_t pow = 1;
        int32_t y = len - i;
        for (int i = 1; i < y; i++) {
            pow = pow * 36;
        }
        tid += ret * pow;
    }
    return tid;
}

void ByteUtil::int64ToSysId(char *data, int64_t tid) {
    int64_t num = tid;
    int len = 0;
    char temp[16];
    while (num > 36) {
        int8_t ni = (num % 36) & 0xFF;
        if (ni < 10) {
            temp[len++] = ni + '0';
        } else {
            temp[len++] = ni - 10 + 'A';
        }
        num = num / 36;
    }
    if (num < 10) {
        temp[len] = num + '0';
    }
    else {
        temp[len] = num - 10 + 'A';
    }
    for (int i = 0; i <= len; i++) {
        data[i] = temp[len - i];
    }
}

int16_t ByteUtil::getInt16(const char *data) {
    return (int16_t) (data[0] & 0xFF) << 8 | (data[1] & 0xFF);
}

void ByteUtil::int16ToData(char *data, int16_t i1, int16_t i2, int16_t i3, int16_t i4) {
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

int32_t ByteUtil::getInt32(const char *data) {
    return (int32_t) (data[0] & 0xFF) << 24 | (data[1] & 0xFF) << 16 | (data[2] & 0xFF) << 8 |
           (data[3] & 0xFF);
}

void ByteUtil::int32ToData(char *data, int32_t i1, int32_t i2) {
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

int16_t ByteUtil::byte2int16(const char *data, int32_t offset, bool littleEndian) {
    int16_t value = 0;
    for (int count = 0; count < 2; ++count) {
        int shift = (littleEndian ? count : (1 - count)) << 3;
        value |= ((int16_t) 0xff << shift) & ((int16_t) data[offset + count] << shift);
    }
    return value;
}

void ByteUtil::int16ToBytes(int16_t value, char *data, int offset, bool littleEndian) {
    for (int count = 0; count < 2; ++count) {
        int shift = (littleEndian ? count : (1 - count)) << 3;
        data[count + offset] = (char) (value >> shift & 0xFF);
    }
}

int32_t ByteUtil::byte2int32(const char *data, int32_t offset, bool littleEndian) {
    int32_t value = 0;
    for (int count = 0; count < 4; ++count) {
        int shift = (littleEndian ? count : (3 - count)) << 3;
        value |= ((int32_t) 0xff << shift) & ((int32_t) data[offset + count] << shift);
    }
    return value;
}

void ByteUtil::int32ToBytes(int32_t value, char *data, int offset, bool littleEndian) {
    for (int count = 0; count < 4; ++count) {
        int shift = (littleEndian ? count : (3 - count)) << 3;
        data[count + offset] = (char) (value >> shift & 0xFF);
    }
}

int64_t ByteUtil::byte2int64(const char *data, int32_t offset, bool littleEndian) {
    int64_t value = 0;
    for (int count = 0; count < 8; ++count) {
        int shift = (littleEndian ? count : (7 - count)) << 3;
        value |= ((long) 0xff << shift) & ((long) data[offset + count] << shift);
    }
    return value;
}

void ByteUtil::int64ToBytes(int64_t value, char *data, int offset, bool littleEndian) {
    for (int count = 0; count < 8; ++count) {
        int shift = (littleEndian ? count : (7 - count)) << 3;
        data[count + offset] = (char) (value >> shift & 0xFF);
    }
}
