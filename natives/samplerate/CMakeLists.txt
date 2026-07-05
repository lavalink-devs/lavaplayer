cmake_minimum_required(VERSION 3.10)
project(samplerate C)

# Read samplerate version from versions.properties
file(STRINGS "${CMAKE_CURRENT_SOURCE_DIR}/../versions.properties" _version_lines)
foreach(_line ${_version_lines})
    if(_line MATCHES "^samplerate=(.+)$")
        set(SAMPLERATE_VERSION "${CMAKE_MATCH_1}")
    endif()
endforeach()

if(NOT ${CMAKE_C_COMPILER_ID} STREQUAL "MSVC")
	set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fPIC -fdata-sections -ffunction-sections")
else()
	set(CMAKE_C_FLAGS_RELEASE "/MT")
endif()

include_directories(. include)
add_library(samplerate src/samplerate.c src/src_linear.c src/src_sinc.c src/src_zoh.c)
target_compile_definitions(samplerate PRIVATE PACKAGE="libsamplerate" VERSION="${SAMPLERATE_VERSION}" HAVE_STDBOOL_H=1 ENABLE_SINC_BEST_CONVERTER ENABLE_SINC_MEDIUM_CONVERTER ENABLE_SINC_FAST_CONVERTER)
