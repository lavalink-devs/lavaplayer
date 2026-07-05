set(CMAKE_SYSTEM_NAME Linux)
set(CMAKE_SYSTEM_PROCESSOR aarch64)

# Toolchain path is set at runtime via MUSL_CROSS_PATH env variable.
# Falls back to the expected extraction location used by the CI script.
if(DEFINED ENV{MUSL_CROSS_PATH})
    set(_MUSL_BIN "$ENV{MUSL_CROSS_PATH}/bin")
else()
    set(_MUSL_BIN "/tmp/musl-cross/aarch64-linux-musl-cross/bin")
endif()

set(CMAKE_C_COMPILER "${_MUSL_BIN}/aarch64-linux-musl-gcc")
set(CMAKE_CXX_COMPILER "${_MUSL_BIN}/aarch64-linux-musl-g++")

set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
