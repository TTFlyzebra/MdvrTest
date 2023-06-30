//
// Created by Administrator on 2022/2/16.
//

#include "SysUtil.h"

#include <stdio.h>
#include <string.h>
#include <regex.h>
#include <pthread.h>

int32_t SysUtil::exec(const char *cmd, char *buffer, int32_t maxLen){
    FILE *pfile;
    if((pfile = popen(cmd, "r"))){
        int32_t ret = fread(buffer, 1, maxLen, pfile);
        pclose(pfile);
        return ret;
    }
    return -1;
}

int32_t SysUtil::getCpuTemperature()
{
    char ret[64];
    exec(cpu_shell, ret, 64);
    int32_t temp = 0;
    if(sscanf(ret, "%d", &temp) == 0){
        return 0;
    }else{
        return temp/1000 + (temp%1000>500?1:0);
    }
}

int32_t SysUtil::getGpuTemperature()
{
    char ret[64];
    exec(gpu_shell, ret, 64);
    int32_t temp = 0;
    if(sscanf(ret, "%d", &temp) == 0){
        return 0;
    }else{
        return temp/1000 + (temp%1000>500?1:0);
    }
}

int32_t SysUtil::getPidScoreAdj(int32_t pid)
{
    char ret[64];
    char cmd[128];
    sprintf(cmd, adj_shell, pid);
    exec(cmd, ret, 64);
    int32_t score_adj = 0;
    if(sscanf(ret, "%d", &score_adj) == 0){
        return 0;
    }else{
        return score_adj;
    }
}

int32_t SysUtil::forceStop(char* package)
{
    char ret[256];
    char cmd[256];
    sprintf(cmd, forcestop_shell, package);
    return exec(cmd, ret, 256);
}

void SysUtil::setThreadName(std::thread* p_thread, const char *thread_name)
{
    char name[16] = {0};
    int32_t len = strlen(thread_name);
    memcpy(name, thread_name, len<15 ? len : 15);
    pthread_setname_np(p_thread->native_handle(), name);
}