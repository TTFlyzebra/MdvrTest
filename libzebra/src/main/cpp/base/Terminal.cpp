//
// Created by Administrator on 2022/4/11.
//

#include "Terminal.h"

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

void Terminal::Mic::upWorkingTime()
{

}

bool Terminal::Mic::isOpened()
{
    return false;
}
