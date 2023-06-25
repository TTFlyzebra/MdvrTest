cmake_minimum_required(VERSION 3.22.1)

project("rtmp")

include_directories(${CMAKE_SOURCE_DIR})

add_library(
        rtmp
        SHARED
        rtmp.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        rtmp
        ${log-lib})