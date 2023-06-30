#include "TimeUtil.h"

#if defined(WIN32)
#include <Windows.h>
#elif defined(__unix)
#include <sys/time.h>
#endif

int64_t TimeUtil::uptimeUsec()
{
#if defined(WIN32)
    return GetTickCount64() * 1000LL;
#elif defined(__unix)
    struct timeval mTime;
    gettimeofday(&mTime, nullptr);
    return mTime.tv_sec * 1000000LL + mTime.tv_usec;
#endif
}
