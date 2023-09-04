//
// Created by FlyZebra on 2020/7/20 0020.
//

#ifndef F_ZEBRA_FLYLOG_H
#define F_ZEBRA_FLYLOG_H

#include <android/Log.h>
#include <stdio.h>

#define TAG "ZEBRA-CORE"

#define FLOGV(...) __android_log_print(ANDROID_LOG_VERBOSE,TAG ,__VA_ARGS__)
#define FLOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__)
#define FLOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__)
#define FLOGW(...) __android_log_print(ANDROID_LOG_WARN,TAG ,__VA_ARGS__)
#define FLOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__)

#endif //F_ZEBRA_FLYLOG_H
