//
// Created by Administrator on 2022/4/11.
//

#include "Terminal.h"

#include <utils/Timers.h>

Terminal::Terminal()
{

}

Terminal::~Terminal()
{

}

Terminal::Screen::Screen()
{

}

Terminal::Screen::~Screen()
{

}

Terminal::Camera::Camera()
{

}

Terminal::Camera::~Camera()
{

}

void Terminal::Camera::upHasFixTime()
{
    recvFixTime = systemTime(CLOCK_MONOTONIC) / 1000LL;
}

bool Terminal::Camera::isHasFix()
{
    if(systemTime(CLOCK_MONOTONIC)/1000LL - recvFixTime > 500000LL){
        return false;
    }else{
        return true;
    }
}

Terminal::Sound::Sound()
{

}

Terminal::Sound::~Sound()
{

}

Terminal::Mic::Mic()
: workTime(0)
{

}

Terminal::Mic::~Mic()
{

}

void Terminal::Mic::upWorkTime()
{
    workTime = systemTime(CLOCK_MONOTONIC)/1000LL;
}

bool Terminal::Mic::isOpened()
{
    if(systemTime(CLOCK_MONOTONIC)/1000LL - workTime > 1000000LL){
        return false;
    }else{
        return true;
    }
}

void Terminal::Mic::upHasFixTime()
{
    recvFixTime = systemTime(CLOCK_MONOTONIC)/1000LL;
}

bool Terminal::Mic::isHasFix()
{
    if(systemTime(CLOCK_MONOTONIC)/1000LL - recvFixTime > 200000LL){
        return false;
    }else{
        return true;
    }
}
