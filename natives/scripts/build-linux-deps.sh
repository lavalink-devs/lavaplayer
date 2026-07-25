#!/usr/bin/env bash
# build-linux-deps.sh — Cross-compile all static dependencies for lavaplayer natives.
#
# Usage:
#   ./build-linux-deps.sh <configure-host> [cmake-toolchain-file]
#
# Arguments:
#   configure-host      autotools --host triplet, e.g. aarch64-linux-gnu
#                       Use "native" to build without cross-compilation.
#   cmake-toolchain-file  optional path to a CMake toolchain file for fdk-aac
#
# The script derives CC/AR/RANLIB/STRIP from the host triplet automatically.
# Outputs all static libraries (.a) to <repo-root>/natives/libs/64/
#
# Required env vars (set by CI or caller):
#   NATIVES_DIR   — absolute path to the natives/ directory

set -euo pipefail

CONFIGURE_HOST="${1:-native}"
TOOLCHAIN_FILE="${2:-}"

: "${NATIVES_DIR:=$(cd "$(dirname "$0")/.." && pwd)}"

LIBS_DIR="$NATIVES_DIR/libs/64"
mkdir -p "$LIBS_DIR"

# ---------------------------------------------------------------------------
# Derive cross-compilation tools from the host triplet
# ---------------------------------------------------------------------------
if [ "$CONFIGURE_HOST" = "native" ]; then
    CC="${CC:-gcc}"
    CXX="${CXX:-g++}"
    AR="${AR:-ar}"
    RANLIB="${RANLIB:-ranlib}"
    STRIP="${STRIP:-strip}"
    HOST_FLAG=""
else
    CC="${CC:-${CONFIGURE_HOST}-gcc}"
    CXX="${CXX:-${CONFIGURE_HOST}-g++}"
    AR="${AR:-${CONFIGURE_HOST}-ar}"
    RANLIB="${RANLIB:-${CONFIGURE_HOST}-ranlib}"
    STRIP="${STRIP:-${CONFIGURE_HOST}-strip}"
    HOST_FLAG="--host=${CONFIGURE_HOST}"
fi

export CC CXX AR RANLIB STRIP

COMMON_FLAGS="-fPIC -O3 -fdata-sections -ffunction-sections"
export CFLAGS="$COMMON_FLAGS"
export CXXFLAGS="$COMMON_FLAGS"

echo "==> CC=$CC  HOST=$CONFIGURE_HOST"
echo "==> LIBS_DIR=$LIBS_DIR"

# ---------------------------------------------------------------------------
# 1. libogg
# ---------------------------------------------------------------------------
# OGG_INSTALL must be defined before configure so --prefix is baked in at
# configure time (not just at install time); ogg.pc stores the prefix.
OGG_INSTALL="$NATIVES_DIR/build/ogg-install-${CONFIGURE_HOST//\//-}"
OGG_SRC="$NATIVES_DIR/vorbis/libogg"
if [ ! -f "$OGG_INSTALL/lib/libogg.la" ]; then
    echo "==> Configuring, building, and installing libogg..."
    pushd "$OGG_SRC"
    ./configure --enable-static --disable-shared --with-pic \
        --disable-maintainer-mode \
        --prefix="$OGG_INSTALL" \
        $HOST_FLAG \
        CFLAGS="$CFLAGS"
    make -j"$(nproc 2>/dev/null || sysctl -n hw.logicalcpu 2>/dev/null || echo 4)"
    make install
    popd
fi
if [ ! -f "$LIBS_DIR/libogg.a" ]; then
    cp "$OGG_INSTALL/lib/libogg.a" "$LIBS_DIR/"
fi

# ---------------------------------------------------------------------------
# 2. libvorbis
# ---------------------------------------------------------------------------
VORBIS_SRC="$NATIVES_DIR/vorbis/libvorbis"
if [ ! -f "$LIBS_DIR/libvorbis.a" ]; then
    echo "==> Configuring and building libvorbis..."
    pushd "$VORBIS_SRC"
    # PKG_CONFIG_PATH lets pkg-config find ogg.pc from our staging prefix;
    # LDFLAGS ensures AC_CHECK_LIB(ogg, ...) can also link against libogg.
    PKG_CONFIG_PATH="$OGG_INSTALL/lib/pkgconfig${PKG_CONFIG_PATH:+:$PKG_CONFIG_PATH}" \
    ./configure --enable-static --disable-shared --with-pic \
        --disable-maintainer-mode \
        $HOST_FLAG \
        --with-ogg="$OGG_INSTALL" \
        CFLAGS="$CFLAGS" \
        LDFLAGS="-L$OGG_INSTALL/lib"
    # Build libvorbis.la explicitly to skip noinst_PROGRAMS (test_sharedbook)
    # which fails on macOS with newer Xcode (-force_cpusubtype_ALL removed in ld)
    make -C lib libvorbis.la -j"$(nproc 2>/dev/null || sysctl -n hw.logicalcpu 2>/dev/null || echo 4)"
    cp lib/.libs/libvorbis.a "$LIBS_DIR/"
    popd
fi

# ---------------------------------------------------------------------------
# 3. opus
# ---------------------------------------------------------------------------
OPUS_SRC="$NATIVES_DIR/opus/opus"
if [ ! -f "$LIBS_DIR/libopus.a" ]; then
    echo "==> Configuring and building opus..."
    pushd "$OPUS_SRC"
    # Soft-float ABI (arm-linux-gnueabi) cannot use NEON intrinsics; disable them
    # to prevent opus from enabling them based on the x86_64 build host's FPU.
    OPUS_EXTRA_FLAGS=""
    if [ "$CONFIGURE_HOST" = "arm-linux-gnueabi" ]; then
        OPUS_EXTRA_FLAGS="--disable-intrinsics"
    fi
    ./configure --enable-static --disable-shared --with-pic \
        --disable-maintainer-mode \
        $HOST_FLAG \
        $OPUS_EXTRA_FLAGS \
        CFLAGS="$CFLAGS" CXXFLAGS="$CXXFLAGS"
    make clean
    make -j"$(nproc 2>/dev/null || sysctl -n hw.logicalcpu 2>/dev/null || echo 4)"
    cp .libs/libopus.a "$LIBS_DIR/"
    popd
fi

# ---------------------------------------------------------------------------
# 4. mpg123
# ---------------------------------------------------------------------------
# Use CMake (available since mpg123 1.32) to avoid autotools regeneration issues:
# mpg123 1.33.x tarballs have m4/ files with newer mtimes than configure, causing
# make to invoke autoconf which fails without libtool m4 macros on CI runners.
MPG123_SRC="$NATIVES_DIR/mp3/mpg123"
if [ ! -f "$LIBS_DIR/libmpg123.a" ]; then
    echo "==> Building mpg123..."
    MPG123_BUILD="$NATIVES_DIR/build/mpg123-build-${CONFIGURE_HOST//\//-}"
    mkdir -p "$MPG123_BUILD"
    pushd "$MPG123_BUILD"
    TOOLCHAIN_ARGS=""
    [ -n "$TOOLCHAIN_FILE" ] && TOOLCHAIN_ARGS="-DCMAKE_TOOLCHAIN_FILE=$TOOLCHAIN_FILE"
    cmake "$MPG123_SRC/ports/cmake" \
        -DCMAKE_BUILD_TYPE=Release \
        -DBUILD_SHARED_LIBS=OFF \
        -DBUILD_LIBOUT123=OFF \
        -DBUILD_PROGRAMS=OFF \
        -DCMAKE_C_FLAGS="$CFLAGS" \
        $TOOLCHAIN_ARGS
    cmake --build . -j"$(nproc 2>/dev/null || sysctl -n hw.logicalcpu 2>/dev/null || echo 4)"
    find . -name "libmpg123.a" -exec cp {} "$LIBS_DIR/" \;
    popd
fi

# ---------------------------------------------------------------------------
# 5. libsamplerate
# ---------------------------------------------------------------------------
SAMPLERATE_SRC="$NATIVES_DIR/samplerate"
if [ ! -f "$LIBS_DIR/libsamplerate.a" ]; then
    echo "==> Configuring and building libsamplerate..."
    SAMPLERATE_BUILD="$NATIVES_DIR/build/samplerate-build-${CONFIGURE_HOST//\//-}"
    mkdir -p "$SAMPLERATE_BUILD"
    pushd "$SAMPLERATE_BUILD"
    TOOLCHAIN_ARGS=""
    [ -n "$TOOLCHAIN_FILE" ] && TOOLCHAIN_ARGS="-DCMAKE_TOOLCHAIN_FILE=$TOOLCHAIN_FILE"
    cmake "$SAMPLERATE_SRC" \
        -DCMAKE_BUILD_TYPE=Release \
        -DBUILD_SHARED_LIBS=OFF \
        -DCMAKE_C_FLAGS="$CFLAGS" \
        -DLIBSAMPLERATE_EXAMPLES=OFF \
        $TOOLCHAIN_ARGS
    cmake --build . -j"$(nproc 2>/dev/null || sysctl -n hw.logicalcpu 2>/dev/null || echo 4)"
    find . -name "libsamplerate.a" -exec cp {} "$LIBS_DIR/" \;
    popd
fi

# ---------------------------------------------------------------------------
# 6. fdk-aac
# ---------------------------------------------------------------------------
FDKAAC_SRC="$NATIVES_DIR/fdk-aac"
if [ ! -f "$LIBS_DIR/libfdk-aac.a" ]; then
    echo "==> Building fdk-aac..."
    FDKAAC_BUILD="$NATIVES_DIR/build/fdk-aac-build-${CONFIGURE_HOST//\//-}"
    mkdir -p "$FDKAAC_BUILD"
    pushd "$FDKAAC_BUILD"
    TOOLCHAIN_ARGS=""
    [ -n "$TOOLCHAIN_FILE" ] && TOOLCHAIN_ARGS="-DCMAKE_TOOLCHAIN_FILE=$TOOLCHAIN_FILE"
    cmake "$FDKAAC_SRC" \
        -DCMAKE_BUILD_TYPE=Release \
        -DBUILD_SHARED_LIBS=OFF \
        -DCMAKE_C_FLAGS="$CFLAGS" \
        -DCMAKE_CXX_FLAGS="$CXXFLAGS" \
        $TOOLCHAIN_ARGS
    cmake --build . -j"$(nproc 2>/dev/null || sysctl -n hw.logicalcpu 2>/dev/null || echo 4)"
    find . -name "libfdk-aac.a" -exec cp {} "$LIBS_DIR/" \;
    popd
fi

echo "==> All deps built successfully."
ls -lh "$LIBS_DIR/"
