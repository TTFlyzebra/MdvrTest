#include "User.h"

#include "utils/SysUtil.h"

int64_t getUid() 
{
    int64_t uid = 1;
    //char uuid[16];
    //SysUtil::getBiosUUID(uuid);
    //memcpy(&uid, uuid, 8);
    return uid;
}