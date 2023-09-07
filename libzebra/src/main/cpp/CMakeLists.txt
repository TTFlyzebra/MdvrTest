cmake_minimum_required(VERSION 3.22.1)

project("zebra")

include_directories(
        ${CMAKE_SOURCE_DIR}
        ${CMAKE_SOURCE_DIR}/include)

add_library(
        zebra
        SHARED
        Fzebra_jni.cpp
        Fzebra.cpp
        FzebraCB.cpp
        base/Notify.cpp
        base/BaseNotify.cpp
        base/Global.cpp
        base/Terminal.cpp
        base/User.cpp
        buffer/LoopBuf.cpp
        buffer/BufferManager.cpp
        utils/AudioUtil.cpp
        utils/Base64.cpp
        utils/ByteUtil.cpp
        utils/SysUtil.cpp
        utils/YuvUtil.cpp
        utils/TimeUtil.cpp
        rtsp/RtspServer.cpp
        rtsp/RtspClient.cpp
        server/UserServer.cpp
        server/UserClient.cpp
        client/UserSession.cpp
        media/VideoDecoder.cpp
        media/AudioDecoder.cpp
        service/ScreenService.cpp
        service/SndOutService.cpp)

add_library(
        avcodec-57
        SHARED
        IMPORTED)

set_target_properties(
        avcodec-57
        PROPERTIES
        IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libavcodec-57.so)

add_library(
        avdevice-57
        SHARED
        IMPORTED)

set_target_properties(
        avdevice-57
        PROPERTIES
        IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libavdevice-57.so)

add_library(
        avfilter-6
        SHARED
        IMPORTED)

set_target_properties(
        avfilter-6
        PROPERTIES
        IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libavfilter-6.so)

add_library(
        avformat-57
        SHARED
        IMPORTED)

set_target_properties(
        avformat-57
        PROPERTIES
        IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libavformat-57.so)

add_library(
        avutil-55
        SHARED
        IMPORTED)

set_target_properties(
        avutil-55
        PROPERTIES
        IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libavutil-55.so)

add_library(
        swresample-2
        SHARED
        IMPORTED)

set_target_properties(
        swresample-2
        PROPERTIES
        IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libswresample-2.so)

add_library(
        swscale-4
        SHARED
        IMPORTED)

set_target_properties(
        swscale-4
        PROPERTIES
        IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libswscale-4.so)

add_library(
        postproc-54
        SHARED
        IMPORTED)

set_target_properties(
        postproc-54
        PROPERTIES
        IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libpostproc-54.so)

add_library(
        yuv
        SHARED
        IMPORTED)

set_target_properties(
        yuv
        PROPERTIES
        IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libyuv.so)

target_link_libraries(
        zebra
        avcodec-57
        avdevice-57
        avfilter-6
        avformat-57
        avutil-55
        swresample-2
        swscale-4
        postproc-54
        yuv
        android
        log)