//
// Created by FlyZebra on 2021/9/16 0016.
//

#ifndef F_ZEBRA_USERSERVER_H
#define F_ZEBRA_USERSERVER_H

#include "base/BaseNotify.h"

class UserClient;

class UserServer :public BaseNotify {
public:
    UserServer(Notify* notify);
    ~UserServer();   
    void disconnectClient(UserClient* client);

private:
    void serverSocket();
    void removeClient();

private:
    int32_t server_socket;

    std::thread* server_t;
    std::list<UserClient*> users_clients;
    std::mutex mlock_users;

    std::thread* remove_t;
    std::vector<UserClient*> remove_clients;
    std::mutex mlock_remove;
    std::condition_variable mcond_remove;
};

#endif //F_ZEBRA_USERSERVER_H
