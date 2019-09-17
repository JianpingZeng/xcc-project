#!/bin/sh -f
BENCH_BIN=../bin/decode
BENCH_OPT="-4 -l -f"
BENCH_INP=../data/clinton.g721
BENCH_OUT=
BENCH_ARG="${BENCH_OPT} ${BENCH_INP}"
#
time -p ../../../simplesim-2.0/sim-profile -iclass -iprof -brprof ${BENCH_BIN} ${BENCH_ARG} 

