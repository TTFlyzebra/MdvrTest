//
// Created by Administrator on 2022/2/16.
//

#ifndef F_ZEBRA_SYSUTIL_H
#define F_ZEBRA_SYSUTIL_H

#include <stdint.h>
#include <thread>

static const char* cpu_shell = "cat sys/class/thermal/thermal_zone0/temp";
static const char* gpu_shell = "cat sys/class/thermal/thermal_zone8/temp";
static const char* adj_shell = "cat proc/%d/oom_score_adj";
static const char* forcestop_shell = "am force-stop %s";

class SysUtil {
public:
    static int32_t exec(const char *cmd, char *buffer, int32_t maxLen);

    static int32_t getCpuTemperature();

    static int32_t getGpuTemperature();

    static int32_t getPidScoreAdj(int32_t pid);

    static int32_t forceStop(char* package);

    static void setThreadName(std::thread* pThread, const char *name);

};



#endif //F_ZEBRA_SYSUTIL_H
