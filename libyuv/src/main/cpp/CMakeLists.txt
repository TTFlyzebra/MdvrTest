cmake_minimum_required(VERSION 3.22.1)

project("flyyuv")
include_directories(
        ${CMAKE_SOURCE_DIR}/include
)

add_library(
        flyyuv
        SHARED
        FlyYuv.cpp
        FlyYuv_jni.cpp)

add_library(
        yuv
        SHARED
        IMPORTED)

set_target_properties(
        yuv
        PROPERTIES
        IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libyuv.so)

target_link_libraries( # Specifies the target library.
        flyyuv
        yuv
        android
        log)