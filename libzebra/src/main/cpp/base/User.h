#ifndef F_ZEBRA_USER_H
#define F_ZEBRA_USER_H

#include <stdint.h>

struct User {
    int64_t uid{ 0 };
    char name[32]{ 0 };

    char sockaddr[32]{ 0 };
    int32_t sockport;
    char peeraddr[32]{ 0 };
    int32_t peerport;
};

int64_t getUid();

#endif //F_ZEBRA_USER_H