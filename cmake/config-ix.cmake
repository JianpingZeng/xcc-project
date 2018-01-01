# By default, we target the host, but this can be overridden at CMake
# invocation time.
include(GetHostTriple)
get_host_triple(XCC_INFERRED_HOST_TRIPLE)

set(XCC_HOST_TRIPLE "${XCC_INFERRED_HOST_TRIPLE}" CACHE STRING
    "Host on which XCC binaries will run")


# Determine the native architecture.
string(TOLOWER "${XCC_TARGET_ARCH}" XCC_NATIVE_ARCH)
if( XCC_NATIVE_ARCH STREQUAL "host" )
  string(REGEX MATCH "^[^-]*" XCC_NATIVE_ARCH ${XCC_HOST_TRIPLE})
endif ()

if (XCC_NATIVE_ARCH MATCHES "i[2-6]86")
  set(XCC_NATIVE_ARCH X86)
elseif (XCC_NATIVE_ARCH STREQUAL "x86")
  set(XCC_NATIVE_ARCH X86)
elseif (XCC_NATIVE_ARCH STREQUAL "amd64")
  set(XCC_NATIVE_ARCH X86)
elseif (XCC_NATIVE_ARCH STREQUAL "x86_64")
  set(XCC_NATIVE_ARCH X86)
elseif (XCC_NATIVE_ARCH MATCHES "sparc")
  set(XCC_NATIVE_ARCH Sparc)
elseif (XCC_NATIVE_ARCH MATCHES "powerpc")
  set(XCC_NATIVE_ARCH PowerPC)
elseif (XCC_NATIVE_ARCH MATCHES "aarch64")
  set(XCC_NATIVE_ARCH AArch64)
elseif (XCC_NATIVE_ARCH MATCHES "arm64")
  set(XCC_NATIVE_ARCH AArch64)
elseif (XCC_NATIVE_ARCH MATCHES "arm")
  set(XCC_NATIVE_ARCH ARM)
elseif (XCC_NATIVE_ARCH MATCHES "mips")
  set(XCC_NATIVE_ARCH Mips)
elseif (XCC_NATIVE_ARCH MATCHES "xcore")
  set(XCC_NATIVE_ARCH XCore)
elseif (XCC_NATIVE_ARCH MATCHES "msp430")
  set(XCC_NATIVE_ARCH MSP430)
elseif (XCC_NATIVE_ARCH MATCHES "hexagon")
  set(XCC_NATIVE_ARCH Hexagon)
elseif (XCC_NATIVE_ARCH MATCHES "s390x")
  set(XCC_NATIVE_ARCH SystemZ)
elseif (XCC_NATIVE_ARCH MATCHES "wasm32")
  set(XCC_NATIVE_ARCH WebAssembly)
elseif (XCC_NATIVE_ARCH MATCHES "wasm64")
  set(XCC_NATIVE_ARCH WebAssembly)
else ()
  message(FATAL_ERROR "Unknown architecture ${XCC_NATIVE_ARCH}")
endif ()

# If build targets includes "host", then replace with native architecture.
list(FIND XCC_TARGETS_TO_BUILD "host" idx)
if( NOT idx LESS 0 )
  list(REMOVE_AT XCC_TARGETS_TO_BUILD ${idx})
  list(APPEND XCC_TARGETS_TO_BUILD ${XCC_NATIVE_ARCH})
  list(REMOVE_DUPLICATES XCC_TARGETS_TO_BUILD)
endif()


