#!/usr/bin/env bash

if [ "$(uname)" == "Darwin" ]; then
    CUR_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
    JOBS=$(sysctl -n hw.ncpu)
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    CUR_DIR=$(dirname $(readlink -f "$0"))
    echo $CUR_DIR
    JOBS=$(grep -c ^processor /proc/cpuinfo)
else
    echo "Unsupported OS, only Linux and MacOS are supported"
    exit 1
fi


TARGET=$1
if [ ! -f "/usr/bin/gcc-4.6" ]; then
    echo "gcc-4.6 and g++-4.6 is required to build dragonegg and llvm 3.0"
    exit 1
fi

if [ "$TARGET" == "ARM" ]; then
    INSTALL_PREFIX=$(pwd)/install/gcc-llvm-xcc-arm-linux-gnueabi

    # decompress arm-unknown-linux-gnu-gcc-4.6.4
    if [ ! -d "$CUR_DIR/arm-unknown-linux-gnueabi" ]; then
      tar -axvf arm-unknown-linux-gnueabi.tar.xz -C ${CUR_DIR}
    fi

    ARM_GCC=$CUR_DIR/arm-unknown-linux-gnueabi/bin/arm-unknown-linux-gnueabi-gcc
    ARM_GPP=$CUR_DIR/arm-unknown-linux-gnueabi/bin/arm-unknown-linux-gnueabi-g++
    X86_GCC=/usr/bin/gcc-4.6
    X86_GPP=/usr/bin/g++-4.6
elif [ "$TARGET" == "X86" ]; then
    INSTALL_PREFIX=$(pwd)/install/gcc-llvm-xcc-x86-64-linux-gnu
    X86_GCC=/usr/bin/gcc-4.6
    X86_GPP=/usr/bin/g++-4.6
else
    echo "unsupported target $1, only x86 and arm supported"
    exit 1
fi

# Targets to build should be one of: X86 PowerPC AArch64
CMAKE_OPTIONS="-DCMAKE_INSTALL_PREFIX=$INSTALL_PREFIX"

echo "===================================================================="
echo "Configuring and Bulding LLVM..."
echo "===================================================================="
# Use local GCC to bootstrap LLVM
if [ ! -d $CUR_DIR/llvm-3.0.src ]; then
    tar -axvf llvm-3.0.tar.gz -C $CUR_DIR
fi

if [ ! -d $CUR_DIR/llvm-3.0.src/build ]; then
    mkdir -p $CUR_DIR/llvm-3.0.src/build
fi

if [ ! -f $CUR_DIR/llvm-3.0.src/build/.compile.${TARGET}.succeeded ]; then
    export CC=$X86_GCC
    export CXX=$X86_GPP
    pushd $CUR_DIR/llvm-3.0.src/build && \
    cmake  ../ -DLLVM_TARGETS_TO_BUILD=$TARGET -DCMAKE_INSTALL_PREFIX=$INSTALL_PREFIX  && \
    cmake --build . -- -j$JOBS         &&  \
    touch .compile.succeeded           &&  \
    cmake --build ./ --target install  &&  \
    popd || exit 1
else
    echo "already configured and built llvm before, do not do it again!"
fi


echo "===================================================================="
echo "Bulding dragonegg-3.0 for ${TARGET}..."
echo "===================================================================="
if [ ! -f $CUR_DIR/dragonegg-3.0.tar.xz ]; then
    echo "dragonegg-3.0.tar.xz does not exist?"
    exit 1
fi
if [ "$TARGET" == "ARM" ]; then
    if [ ! -d "$CUR_DIR/dragonegg-arm" ]; then
        tar -axf dragonegg-3.0.tar.xz && mv dragonegg-3.0 ${CUR_DIR}/dragonegg-arm
    fi

    if [ ! -f $CUR_DIR/dragonegg-arm/.built.succeeded ]; then
        export CC=$X86_GCC
        export CXX=$X86_GPP
        pushd $CUR_DIR/dragonegg-arm
        GCC=$ARM_GCC LLVM_CONFIG=$INSTALL_PREFIX/bin/llvm-config make
        popd || exit 1
    fi
else
    if [ ! -d "$CUR_DIR/dragonegg-x86" ]; then
        tar -axf dragonegg-3.0.tar.xz && mv dragonegg-3.0 ${CUR_DIR}/dragonegg-x86
    fi
    if [ ! -f $CUR_DIR/dragonegg-x86/.built.succeeded ]; then
        export CC=$X86_GCC
        export CXX=$X86_GPP
        pushd $CUR_DIR/dragonegg-x86
        GCC=$X86_GCC LLVM_CONFIG=$INSTALL_PREFIX/bin/llvm-config make
        popd || exit 1
    fi
fi

echo "===================================================================="
echo "Bulding xcc..."
echo "===================================================================="
if [ ! -d $CUR_DIR/xcc ]; then
    echo "xcc does not exist?"
    exit 1
fi
if [ ! -d $CUR_DIR/xcc/cmake-build-debug ]; then
    mkdir -p $CUR_DIR/xcc/cmake-build-debug
fi

if [ ! -f $CUR_DIR/xcc/cmake-build-debug/.config.succeeded ]; then
    export CC=$X86_GCC
    export CXX=$X86_GPP
    pushd ${CUR_DIR}/xcc/cmake-build-debug     && \
    cmake  ../ -DXCC_TARGETS_TO_BUILD=$TARGET  && \
    cmake --build .                  && \
    touch .compile.succeeded         && \
    popd || exit 1
else
    echo "already configured and built xcc before, skip it!"
fi


