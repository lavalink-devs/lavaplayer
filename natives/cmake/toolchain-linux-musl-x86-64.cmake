set(CMAKE_SYSTEM_NAME Linux)
set(CMAKE_SYSTEM_PROCESSOR x86_64)

# musl-gcc is provided by the musl-tools apt package on Ubuntu
set(CMAKE_C_COMPILER musl-gcc)
# musl-tools does not include a C++ wrapper; use system g++ for C++ sources
# (connector is C-only so this is fine)
set(CMAKE_CXX_COMPILER g++)

set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
