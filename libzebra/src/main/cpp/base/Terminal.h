//
// Created by Administrator on 2022/3/6.
//

#ifndef F_ZEBRA_TERMINAL_H
#define F_ZEBRA_TERMINAL_H

#include <stdint.h>

class Terminal{
public:
    Terminal();
    ~Terminal();

    class Screen{
    public:
        Screen();
        ~Screen();
    public:
        uint16_t w{ 640 };
        uint16_t h{ 1280 };
        uint8_t orientation{ 0 };
        uint16_t dpi{ 640 };
        uint8_t fmt{ 0 };
        uint8_t fps{ 16 };
        uint8_t lev{ 8 };
    };

    class Camera{
    public:
        Camera();
        ~Camera();
        void upHasFixTime();
        bool isHasFix();
    public:
        uint16_t w{ 720 };
        uint16_t h{ 1280 };
        uint8_t fmt{ 0 };
        uint8_t fps{ 16 };
        uint8_t lev{ 8 };
        bool outyuv{ false };
        bool fixyuv{ false };
        bool hasLocal{ false };
        int64_t recvFixTime;
    };

    class Sound{
    public:
        Sound();
        ~Sound();
    public:
        bool outpcm{ false };
        bool mute { true };
    };

    class Mic{
    public:
        Mic();
        ~Mic();
        void upWorkTime();
        bool isOpened();
        void upHasFixTime();
        bool isHasFix();
    private:
        int64_t workTime;
        int64_t recvFixTime;
    public:
        bool outpcm{ false };
        bool fixpcm{ false };
    };

    Screen screen;
    Camera camera;
    Sound sound;
    Mic mic;

    uint64_t tid{ 0 };
    char model[16]{ 0 };
    char sockaddr[32]{ 0 };
    int32_t sockport;
    char peeraddr[32]{ 0 };
    int32_t peerport;

    uint64_t totalRecv;
    uint64_t totalSend;
    uint64_t recvFreq[0xFFFF]{ 0 };
    uint64_t recvSize[0xFFFF]{ 0 };
    uint64_t sendFreq[0xFFFF]{ 0 };
    uint64_t sendSize[0xFFFF]{ 0 };
};

#endif //F_ZEBRA_TERMINAL_H