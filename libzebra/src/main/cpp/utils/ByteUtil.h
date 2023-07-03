//
// Created by Administrator on 2022/2/12.
//

#ifndef F_ZEBRA_BYTEUTIL_H
#define F_ZEBRA_BYTEUTIL_H

#include <stdint.h>

class ByteUtil {
public:
    /**
    * 9位系统序列号字符串转8位int64
    **/
    static int64_t sysIdToInt64(char* data);
    /**
    * 8位int64转为9位系统序列号字符串
    **/
    static void int64ToSysId(char* data, int64_t tid);

    static int16_t getInt16(const char* data);
    static void int16ToData(char* data, int16_t i1, int16_t i2 = 0, int16_t i3 = 0, int16_t i4 = 0);

    static int32_t getInt32(const char* data);
    static void int32ToData(char* data, int32_t i1, int32_t i2 = 0);

    static int16_t byte2int16(const char* data, int32_t offset, bool littleEndian);
    static void int16ToBytes(int16_t value, char* data, int offset, bool littleEndian);

    static int64_t byte2int64(const char* data, int32_t offset, bool littleEndian);
    static void int64ToBytes(int64_t value, char* data, int offset, bool littleEndian);
};

#endif //F_ZEBRA_BYTEUTIL_H
