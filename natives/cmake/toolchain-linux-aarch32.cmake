set(CMAKE_SYSTEM_NAME Linux)
set(CMAKE_SYSTEM_PROCESSOR arm)

set(CMAKE_C_COMPILER arm-linux-gnueabihf-gcc)
set(CMAKE_CXX_COMPILER arm-linux-gnueabihf-g++)

# Target ARMv8 in AArch32 mode; neon-fp-armv8 is the correct FPU for armv8-a
# in 32-bit mode and is required for the gnueabihf hard-float ABI to work.
set(CMAKE_C_FLAGS_INIT "-march=armv8-a -mfpu=neon-fp-armv8")
set(CMAKE_CXX_FLAGS_INIT "-march=armv8-a -mfpu=neon-fp-armv8")

set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
