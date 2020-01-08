#!/bin/bash

# IMPORTANT: Set these variables in accordance with your local environment
LLVM="${HOME}/Development/Compiler/xcc/cmake-build-debug"
X86_GCC="${HOME}/Development/Compiler/GCC-4.6.4"
X86_DRAGONEGG="${HOME}/Development/Compiler/dragonegg-3.0"

ARM_DRAGONEGG="${HOME}/Development/Compiler/dragonegg-3.0"
ARM_GCC="${HOME}/Development/Research/arm-unknown-linux-gnueabi/bin"

# -----------------------------------------------------------------------------
# Helpers 
# -----------------------------------------------------------------------------

__debug_lvl=1
DOut() {
  if (( $1 <= __debug_lvl )); then
    shift
    echo -e $*
  fi
}

DCall() {
  DOut 1 '>> ' $*
  eval "$*"
  return $?
}

Die() {
  echo "!!!" $*
  exit 1
}

DumpARGV() {
  local i=0
  for argv in $*; do
    echo "argv "$((i++))":" $argv
  done
  return -1
}

SetGCCOpts() {
  __gcc_args=$1; shift
  declare -a gcc_options
  unset gcc_options
  for key in $*; do
    eval "local flag_vals=\${FLAG_${key}[*]}"
    eval "local flag_knob=\${FLAG_KNOB_${key}}"
    eval "local flag_hasval=\${FLAG_HASVAL_${key}}"
    if [[ -n $flag_vals ]]; then
      # If no value associated with this flag, just give the flag knob
      if [[ -z $flag_hasval ]]; then
        gcc_options[${#gcc_options[*]}]=${flag_knob}
      else
        # Some options need a separator such as "=".  Assume this is true for
        # any with a knob that is more than one character long after the dash.
        if (( ${#flag_knob} > 2 )); then
          flag_knob=${flag_knob}"="
        fi
        # Some GCC arguments like -I, -l, etc. are given multiple times.
        for flag_val in ${flag_vals}; do
          gcc_options[${#gcc_options[*]}]=${flag_knob}${flag_val}
        done
      fi
    fi
  done
  eval "${__gcc_args}=\"${gcc_options[*]}\""
}

# -----------------------------------------------------------------------------
# FLAGS module
# -----------------------------------------------------------------------------

__FLAGS_ParseValForKey() {
  local key=$1
  local __val=$2
  local arg=$3
  local next_arg=$4
  eval "local hasval=\${FLAG_HASVAL_${key}}"
  eval "local knob=\${FLAG_KNOB_${key}}"

  if [[ -z $hasval ]]; then
    eval "$__val=$arg"
    DOut 2 "Option takes no arguments; using arg"
    return 1
  fi

  if (( ${#arg} == ${#knob} )); then
    eval "$__val=${next_arg}"
    DOut 2 "Read value from next arg:" $next_arg
    return 2
  fi

  if [[ ${arg:${#knob}:1} == "=" ]]; then
    local arg_val=${arg:${#knob}+1}
    DOut 2 "Read value from this arg after '=':" $arg_val
    eval "$__val=$arg_val"
    return 1
  fi

  local arg_val=${arg:${#knob}}
  DOut 2 "Read value from this arg:" $arg_val
  eval "$__val=$arg_val"
  return 1
}

__FLAGS_ParseArg() {
  for key in ${__FLAGS_keys[*]}; do
    eval "local knob=\${FLAG_KNOB_${key}}"
    if [[ $knob == ${1:0:${#knob}} ]]; then
      __FLAGS_ParseValForKey $key val $*
      local return_val=$?
      eval "FLAG_${key}[\${#FLAG_${key}[*]}]=${val}"
      return $return_val
    fi
  done

  DOut 2 "Not recognized, moving to argv"
  FLAGS_ARGV[${#FLAGS_ARGV[*]}]=$1
  return 1
}

FLAGS_Parse() {
  # Parse arguments.
  while [[ -n $1 ]]; do
    DOut 2 "Processing:" $1
    __FLAGS_ParseArg $*
    values_parsed=$?
    for ((i=0; i < values_parsed; i++)); do
      DOut 3 "Shifting..."
      shift
    done
  done

  # Check required flags are set.
  for key in ${__FLAGS_keys[*]}; do
    flag_vals="FLAG_${key}"
    flag_knob="FLAG_KNOB_${key}"
    flag_required="FLAG_REQUIRED_${key}"
    if [[ -n ${!flag_required} && -z ${!flag_vals} ]]; then
      Die "Required option" ${!flag_knob} "missing!"
    fi
  done
}

FLAGS_Define() {
  local key=$1
  local knob=$2
  local hasval=$3
  local required=$4

  __FLAGS_keys[${#__FLAGS_keys[*]}]=${key}
  eval "FLAG_KNOB_${key}=$knob"
  eval "FLAG_HASVAL_${key}=$hasval"
  eval "FLAG_REQUIRED_${key}=$required"
}

# -----------------------------------------------------------------------------
# LLVM/GCC driver code
# -----------------------------------------------------------------------------

# Required.
FLAGS_Define 'output' '-o' 'has_vals' ''


# For ARM set --target=armv7 (x86 otherwise)
FLAGS_Define 'target' '--target' 'has_vals' ''

# To link C++ code set --linker=c++, link fortran using --linker=fortran (C only otherwise)
FLAGS_Define 'linker' '--linker' 'has_vals' ''


# Optional gcc.
FLAGS_Define 'compile_only' '-c' '' ''
FLAGS_Define 'assemble_only' '-S' '' ''
FLAGS_Define 'debug' '-g' '' ''
FLAGS_Define 'preprocess_only' '-E' '' ''
FLAGS_Define 'define' '-D' 'has_vals' ''
FLAGS_Define 'include' '-I' 'has_vals' ''
FLAGS_Define 'libpath' '-L' 'has_vals' ''
FLAGS_Define 'library' '-l' 'has_vals' ''
FLAGS_Define 'noexceptions' '-fno-exceptions' '' ''
FLAGS_Define 'optimization' '-O' 'has_vals' ''
FLAGS_Define 'permissive' '-fpermissive' '' ''
FLAGS_Define 'prefetch' '-fprefetch-loop-arrays' '' ''
FLAGS_Define 'standard' '-std' 'has_vals' ''
FLAGS_Define 'static' '-static' '' ''
FLAGS_Define 'unroll' '-funroll-loops' '' ''
FLAGS_Define 'pthread' '-pthread' '' ''
FLAGS_Define 'openmp' '-fopenmp' '' ''
FLAGS_Define 'nostrictaliasing' '-fno-strict-aliasing' '' ''
FLAGS_Define 'unsignedchar' '-funsigned-char' '' ''
FLAGS_Define 'nounsafemathopt' '-fno-unsafe-math-optimizations' '' '' 
FLAGS_Define 'gnu89inline' '-fgnu89-inline' '' ''
FLAGS_Define 'bigendian' '-fconvert=big-endian' '' ''
FLAGS_Define 'pedantic' '-pedantic' '' ''
FLAGS_Define 'W' '-W' 'has_vals' ''

#for compiling the uclibc
#FLAGS_Define 'wall' '-Wall' '' ''


# Optional llc.
#FLAGS_Define 'verify_all' '--verify-machineinstrs' '' ''

# Parse arguments and reset argv to remaining options.
FLAGS_Parse "$@"
eval "set -- ${FLAGS_ARGV[*]}"

# Acknowledge limitations.
if [ -n "$FLAG_compile_only" ]; then
  compile=1
  #[[ -z $2 ]] || DumpARGV $* || Die "Only one ARGV supported for just compile!"
else
  compile=0
fi

if [ -n "$FLAG_assemble_only" ]; then
  assemble=1
else
  assemble=0
fi

#echo "FLAG_compile_only:$FLAG_compile_only FLAG_preprocess_only:  $FLAG_preprocess_only"
if [ -n "$FLAG_compile_only" ] || [ -n "$FLAG_preprocess_only" ] || [ -n "$FLAG_assemble_only" ]; then
  link=0
  [[ -z $2 ]] || DumpARGV $* || Die "Only one ARGV supported for just compile!"
else
  link=1
fi


# This function used to check whether the source file is a valid file according to it's file 
# extension.
function is_source_file() {
  if [[ "$1" == "c" || "$1" == "C" || "$1" == "h" || "$1" == "hpp" || \
        "$1" == "hxx" || "$1" == "cpp" || "$1" == "cxx" || "$1" == "cc" || \
        "$1" == "CC" || "$1" == "f" || "$1" == "f90" || "$1" == "F" ]]; then
    return 1
  else
    return 0
  fi
}

# Sort out compile args from linker args.
for i in $*; do
  # Get the extension
  ext=${i##*.}
  is_source_file $ext
  valid=$?
  if (( $valid == 0 )); then
    if [[ "$ext" == "o" ]]; then
      #[[ $i == *.o ]] || Die "Unrecognized argument or file type: '$i'!"
      (( link ))      || Die ".o file given but linking turned off!"
      objs="$objs $i"
    elif [[ "$ext" == "ll" ]]; then
      (( compile ))      || Die ".ll file given but compiling turned off!"
      llcs="$llcs $i"
    elif [[ "$ext" == "a" ]]; then
      # add support for static linking library
      (( link ))    || Die ".a file given but linking turned off!"
      static_libs="$static_libs $i"  
    else
      Die "Unrecognized argument or file type: '$i'!"
    fi
  else
    srcs="$srcs $i"
  fi
done

# Determine terget-specific stuff.
# Currently, xcc llc doesn't support arm target.
if [[ $FLAG_target == "armv7" ]]; then
  GCC="${ARM_GCC}/arm-unknown-linux-gnueabi-gcc"
  GPP="${ARM_GCC}/arm-unknown-linux-gnueabi-g++"
  GFORTRAN="${ARM_GCC}/arm-unknown-linux-gnueabi-gfortran"
  DRAGONEGG="${ARM_DRAGONEGG}/dragonegg.so"
  CPU="-march=arm"
#  GCCCPU="-ccc-host-triple arm-linux-gnueabi"
else
  GCC="${X86_GCC}/bin/gcc"
  GPP="${X86_GCC}/bin/g++"
  GFORTRAN="${X86_GCC}/bin/gfortran"
  GFORTRAN="${X86_GCC}/bin/gfortran"
  DRAGONEGG="${X86_DRAGONEGG}/dragonegg.so"
  CPU="-march=x86-64"
#  GCCCPU="-mtune=corei7-avx"
fi

# FIXME: These passes, which run after register allocation in LLVM, potentially
# mess up the idempotence invariant maintained during register allocation.  If
# anyone is serious about using the idempotence compilation, they need to modify
# these passes to make them idempotence-aware.
# Following two lines was commented by Jianping Zeng on 3/6/2019
#LLC_DAMAGE_CONTROL="-disable-tail-calls -disable-postra-machine-licm \
#-disable-copyprop -disable-post-ra"

#-disable-copyprop -disable-post-ra -disable-lsr"

# X86 NOTE: any x87 floating point stuff is not going to work with idempotence
# (see X86FloatingPoint.cpp).  Using it is deprecated so no big deal; x86-64
# using SSE will work fine, although 'long double' (80-bit FP) still uses the
# x87 FP stack which is broken. 

#echo "link flag:  $link   compile flag: $compile"

# Preprocess.
for src in $srcs; do
  if (( link )); then
    llc=${src%.[cCfF]*}.ll
  else
    if (( compile )) || (( assemble )); then
        if [ -z "${FLAG_output}" ]; then
            llc=${src%.[cCfF]*}.ll
        else
            if (( compile )); then
                llc=${FLAG_output%.o}.ll
            else
                llc=${FLAG_output%.s}.ll
            fi
        fi
    else
      llc=${FLAG_output}
    fi
  fi

  llcs="$llcs $llc"

  DOut 1 "\nCompiling $src --> $llc"

  SetGCCOpts gcc_front_opts "include" "standard" "optimization" "define" \
      "noexceptions" "permissive" "prefetch" "unroll" "strict_aliasing"  \
      "nostrictaliasing" "unsignedchar" "unsignedchar" "nounsafemathopt" \
      "gnu89inline" "bigendian" "debug"

  DOut 2 "GCC front-end options:" $gcc_front_opts

  DCall "${GCC} \
  -fplugin=${DRAGONEGG} \
  -fplugin-arg-dragonegg-emit-ir -O2 -S \
  ${src} -o ${llc} \
  ${gcc_front_opts}" || Die "GCC front-end compile failed!"

#  DCall "${LLVM}/bin/opt -mem2reg -loops -loop-simplify -loop-rotate -indvars -loop-unroll -unroll-count=3 ${llc} -S -o ${llc}" || Die "LLVM opt loop unroll failed!!"
#  DCall "${LLVM}/bin/opt -mem2reg -loops -loop-simplify -loop-rotate -indvars -loop-unroll -unroll-count=4 ${llc} -S -o ${llc}" || Die "LLVM opt loop unroll failed!!"
#  DCall "${LLVM}/bin/opt -mem2reg -indvars -loop-unroll -unroll-count=4 ${llc} -S -o ${llc}" || Die "LLVM opt loop unroll failed!!"
#  DCall "${LLVM}/bin/opt -mem2reg -indvars -loop-unroll -unroll-count=8 ${llc} -S -o ${llc}" || Die "LLVM opt loop unroll failed!!"
#  DCall "${LLVM}/bin/opt -dse ${llc} -S -o ${llc}" || Die "LLVM opt basicaa failed!" 

#  DCall "${LLVM}/bin/llvm-as ${llc} -o ${byc}" || Die "LLVM llvm-as failed!"
done

# Compile.
if [ $compile -ne 0 ] || [ $link -ne 0 ] || [ $assemble -ne 0 ]; then
  for llfile in $llcs; do
    if (( link )); then
      obj=${llfile%.[ll]*}.o
      asm=${llfile%.[ll]*}.s
      objs="$objs $obj"
    else
        if [ -z "${FLAG_output}" ]; then            
            obj=${llfile%.[ll]*}.o
            llc=${obj%.o*}.ll
            asm=${obj%.o*}.s
        else
            if (( compile )); then
                obj=${FLAG_output}
                llc=${obj%.o*}.ll
                asm=${obj%.o*}.s
            else
                asm=${FLAG_output}
                llc=${asm%.s*}.ll
                obj=${asm%.s*}.o
            fi
        fi
    fi

    DOut 1 "\nCompiling $llfile --> $asm"


    DCall "${LLVM}/bin/llc ${llfile} -o ${asm} \
    $LLC_DAMAGE_CONTROL $CPU ${FLAG_verify} ${FLAG_verify_all}" || Die "LLVM llc failed!"
  
    SetGCCOpts gcc_back_opts "optimization"
    DOut 2 "GCC back-end options:" $gcc_back_opts
  
  #change for static compilation 2.
    if (( compile )) || (( link )); then
        DOut 1 "\nAssembling $asm --> $obj"

        DCall "${GCC} \
        ${asm} -c -o ${obj} $GCCCPU \
        ${gcc_back_opts}" || Die "GCC back-end compile failed!"
      #  ${gcc_back_opts} -static" || Die "GCC back-end compile failed!"
    fi
  done
fi


#Link.
if (( link )); then
  [[ -n $objs ]] || Die "no object files!"

  linker=${GCC}
  if [[ $FLAG_linker == "c++" ]]; then
    linker=${GPP}
  elif [[ $FLAG_linker == "fortran" ]]; then
    linker=${GFORTRAN}
  fi
  DOut 1 "\nLinking $objs --> $FLAG_output"

  SetGCCOpts gcc_link_opts "libpath" "library" "static" "pthread"
  DOut 2 "GCC link options:" $gcc_link_opts

  DCall "$linker \
  ${objs} ${static_libs} -o ${FLAG_output} \
  ${gcc_link_opts}" || Die "GCC linking failed!"
fi


