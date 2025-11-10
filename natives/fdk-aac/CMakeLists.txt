cmake_minimum_required(VERSION 3.0)

project(fdk-aac CXX)

get_filename_component(ROOT_DIR "." ABSOLUTE)

set(FDK_SOURCE "${ROOT_DIR}/fdk-aac-2.0.2")

set(FDK_DIRS "${FDK_SOURCE}/libAACdec" "${FDK_SOURCE}/libFDK" "${FDK_SOURCE}/libSYS" "${FDK_SOURCE}/libMpegTPDec" "${FDK_SOURCE}/libSBRdec" "${FDK_SOURCE}/libPCMutils" "${FDK_SOURCE}/libArithCoding" "${FDK_SOURCE}/libDRCdec" "${FDK_SOURCE}/libSACdec")
set(MAIN_SOURCES "")

message(STATUS "${FDK_DIRS}")

foreach(subdir ${FDK_DIRS})
	file (GLOB dir_sources "${subdir}/src/*.cpp" "${subdir}/src/*.h" "${subdir}/include/*.h")
	set (MAIN_SOURCES ${MAIN_SOURCES} ${dir_sources})
	
	include_directories("${subdir}/include")
endforeach()

if ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "MSVC")
	add_definitions(-Dinline=__inline)
	set(CMAKE_CXX_FLAGS_RELEASE "/MT")
else()
	add_definitions(-Wno-narrowing)
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fPIC -fdata-sections -ffunction-sections")
endif()

add_library(fdk-aac STATIC ${MAIN_SOURCES})
