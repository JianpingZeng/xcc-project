#!/bin/sh -f
BENCH_BIN=../src/gs
BENCH_OPT="-sDEVICE=ppm -dNOPAUSE -q"
BENCH_INP="../data/tiger.ps"
BENCH_OUT="-sOutputFile=../data/test.ppm"
BENCH_ARG="${BENCH_OPT} ${BENCH_OUT} -- ${BENCH_INP}"
#-sDEVICE=ppm -sOutputFile=../data/test.ppm -dNOPAUSE -q -- ../data/tiger.ps#
../../../simplesim-2.0/sim-profile -iclass -iprof -brprof ${BENCH_BIN} ${BENCH_ARG}

