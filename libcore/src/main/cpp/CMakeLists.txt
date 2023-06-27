cmake_minimum_required(VERSION 3.22.1)

project("zebra")

include_directories(${CMAKE_SOURCE_DIR})

add_library(
        zebra
        SHARED
        zebra.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        zebra
        ${log-lib})