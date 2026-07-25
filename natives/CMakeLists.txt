cmake_minimum_required(VERSION 3.10)
project(lavaplayer-natives C)

# Sub-libraries must be static; connector overrides this with explicit SHARED.
set(BUILD_SHARED_LIBS OFF)

add_subdirectory(samplerate)
add_subdirectory(fdk-aac)
add_subdirectory(vorbis)
add_subdirectory(connector)
