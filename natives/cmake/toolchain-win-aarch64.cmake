set(CMAKE_SYSTEM_NAME Windows)
set(CMAKE_SYSTEM_PROCESSOR ARM64)

set(CMAKE_C_COMPILER clang)
set(CMAKE_C_COMPILER_TARGET aarch64-w64-mingw32)
set(CMAKE_CXX_COMPILER clang++)
set(CMAKE_CXX_COMPILER_TARGET aarch64-w64-mingw32)

# ARM64 sysroot from MSYS2 CLANGARM64 environment.
# When building inside MSYS2 shell, pass the Unix-style path via CLANGARM64_ROOT
# (e.g. CLANGARM64_ROOT=/clangarm64). Outside MSYS2, use a Windows-style path.
if(DEFINED ENV{CLANGARM64_ROOT})
    set(_clangarm64_root "$ENV{CLANGARM64_ROOT}")
else()
    set(_clangarm64_root "/clangarm64")
endif()

# Tell clang/lld where the ARM64 sysroot is so compiler and linker can find
# Windows ARM64 system headers and import libraries (kernel32, etc.).
set(CMAKE_SYSROOT "${_clangarm64_root}")
set(CMAKE_FIND_ROOT_PATH "${_clangarm64_root}")

# Explicitly add the ARM64 lib directory to the linker search path.
# LLD in MinGW mode may not honour --sysroot for library lookup, so we
# set it via linker flags as well to ensure kernel32.a and friends are found.
set(CMAKE_SHARED_LINKER_FLAGS_INIT "-L${_clangarm64_root}/lib")
set(CMAKE_MODULE_LINKER_FLAGS_INIT "-L${_clangarm64_root}/lib")
set(CMAKE_EXE_LINKER_FLAGS_INIT    "-L${_clangarm64_root}/lib")

set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
