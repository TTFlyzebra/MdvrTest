cmake_minimum_required(VERSION 3.22.1)

project("zebra")

include_directories(
        ${CMAKE_SOURCE_DIR}
        ${CMAKE_SOURCE_DIR}/include
)

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
        utils/Base64.cpp
        utils/ByteUtil.cpp
        utils/SysUtil.cpp
        utils/YuvUtil.cpp
        utils/TimeUtil.cpp
        rtsp/RtspServer.cpp
        rtsp/RtspClient.cpp
        server/UserServer.cpp
        server/UserClient.cpp
)

add_library(
        c++
        SHARED
        IMPORTED)

set_target_properties(
        c++
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libC++.so)

add_library(
        jpeg
        SHARED
        IMPORTED)

set_target_properties(
        jpeg
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libjpeg.so)

add_library(
        yuvz
        SHARED
        IMPORTED)

set_target_properties(
        yuvz
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libyuvz.so)

target_link_libraries(
        zebra
        c++
        jpeg
        yuvz
        log)