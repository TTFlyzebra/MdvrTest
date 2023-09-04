//
// Created by Administrator on 2022/3/6.
//

#ifndef F_ZEBRA_TERMINAL_H
#define F_ZEBRA_TERMINAL_H

#include <stdint.h>

class Terminal {
public:
    Terminal();
    ~Terminal();

    class Screen {
    public:
        Screen();
        ~Screen();
    public:
        uint16_t w{ 720 };
        uint16_t h{ 1440 };
        uint8_t orientation{ 0 };
        uint16_t dpi{ 640 };
        uint8_t fmt{ 0 };
        uint8_t fps{ 16 };
        uint8_t lev{ 8 };
        int32_t multictl{ 0 };
    };

    class Camera {
    public:
        Camera();
        ~Camera();
    public:
        uint16_t w{ 720 };
        uint16_t h{ 1280 };
        uint8_t fmt{ 0 };
        uint8_t fps{ 16 };
        uint8_t lev{ 8 };
        int32_t camout{ 0 };
        int32_t camfix{ 0 };
    };

    class Sound {
    public:
        Sound();
        ~Sound();
    public:
        int32_t sndout{ 0 };
    };

    class Mic {
    public:
        Mic();
        ~Mic();
        void upWorkingTime();
        bool isOpened();
    private:
        int64_t workTime{ 0 };
    public:
        int32_t micfix{ 0 };
    };

    Screen screen;
    Camera camera;
    Sound sound;
    Mic mic;

    uint64_t tid{ 0 };
    char model[16]{ 0 };
    uint16_t cpu_temperature;
    uint16_t gpu_temperature;

    char sockaddr[32]{ 0 };
    int32_t sockport{ 0 };
    char peeraddr[32]{ 0 };
    int32_t peerport{ 0 };

    uint64_t totalRecv;
    uint64_t totalSend;
    uint64_t recvFreq[0xFFFF]{ 0 };
    uint64_t recvSize[0xFFFF]{ 0 };
    uint64_t sendFreq[0xFFFF]{ 0 };
    uint64_t sendSize[0xFFFF]{ 0 };
};

#endif //F_ZEBRA_TERMINAL_H