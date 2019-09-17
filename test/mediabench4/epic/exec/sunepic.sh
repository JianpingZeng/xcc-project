#!/bin/sh -f
BENCH_BIN=../bin/unepic
BENCH_OPT=
BENCH_INP=../data/test.image.pgm.E
BENCH_OUT=
BENCH_ARG="${BENCH_INP} ${BENCH_OPT} ${BENCH_OUT}"
#
time -p ../../../simplesim-2.0/sim-profile -iclass -iprof -brprof ${BENCH_BIN} ${BENCH_ARG} 

