cmake_minimum_required(VERSION 3.10.2)
project(vcam)

add_library(yuv STATIC IMPORTED)
set_target_properties(yuv PROPERTIES
    IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libyuv/lib/${ANDROID_ABI}/libyuv.a
)

add_library(vcam_jni SHARED
    vcam_jni.cpp
)

target_include_directories(vcam_jni PRIVATE
    ${CMAKE_SOURCE_DIR}/libyuv/include
)

target_link_libraries(vcam_jni
    yuv
    log
)
