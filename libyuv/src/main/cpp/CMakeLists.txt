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

target_link_libraries( # Specifies the target library.
        flyyuv
        c++
        jpeg
        yuvz
        log)