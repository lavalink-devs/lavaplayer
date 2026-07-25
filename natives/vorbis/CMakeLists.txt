cmake_minimum_required(VERSION 3.10)

project(vorbis C)

get_filename_component(ROOT_DIR "." ABSOLUTE)
set(VORBIS_SOURCE "${ROOT_DIR}/libvorbis/lib")
set(OGG_SOURCE "${ROOT_DIR}/libogg/src")

if ("${CMAKE_C_COMPILER_ID}" STREQUAL "MSVC")
	set(CMAKE_C_FLAGS_RELEASE "/MT")
else()
	set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fPIC -fdata-sections -ffunction-sections")
endif()

file(GLOB MAIN_SOURCES "${VORBIS_SOURCE}/*.c" "${OGG_SOURCE}/*.c")

list(REMOVE_ITEM MAIN_SOURCES "${VORBIS_SOURCE}/psytune.c")
list(REMOVE_ITEM MAIN_SOURCES "${VORBIS_SOURCE}/tone.c")

include_directories("${ROOT_DIR}/libogg/include")
include_directories("${ROOT_DIR}/libvorbis/include")
include_directories("${ROOT_DIR}/libvorbis/lib")

add_library(vorbis STATIC ${MAIN_SOURCES})
