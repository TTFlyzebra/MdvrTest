//
// Created by FlyZebra on 2021/9/2 0002.
//

#ifndef F_ZEBRA_CONFIG_H
#define F_ZEBRA_CONFIG_H

#define MAX_CAM                             4
#define AUDIO_MIMETYPE                      "audio/mp4a-latm"
#define AUDIO_SAMPLE                        48000
#define AUDIO_FORMAT                        AUDIO_FORMAT_PCM_32_BIT
#define AUDIO_IN_CHANNEL                    AUDIO_CHANNEL_IN_STEREO
#define AUDIO_OUT_CHANNEL                   AUDIO_CHANNEL_OUT_STEREO

#define REMOTEPC_SERVER_TCP_PORT            9036
#define TERMINAL_SERVER_UDP_PORT            9037
#define TERMINAL_SERVER_TCP_PORT            9038

#define RTSP_SERVER_TCP_PORT                8554
#define INPUT_SERVER_TCP_PORT               9008

#define VIDEO_MIMETYPE                      "video/avc"
#define DEFAULT_DPI                         720
#define MEMORY_KILL_TIME                    60

#define WAIT_TIME                           5000000LL

#define TERMINAL_MAX_BUFFER                 8388608

#endif //F_ZEBRA_CONFIG_H