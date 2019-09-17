#!/bin/sh -f
BENCH_BIN=../bin/untoast
BENCH_OPT="-fpl"
BENCH_INP=../data/clinton.pcm.run.gsm
BENCH_OUT=
BENCH_ARG="${BENCH_OPT} ${BENCH_INP}"
#
time -p ../../../simplesim-2.0/sim-profile -iclass -iprof -brprof ${BENCH_BIN} ${BENCH_ARG} 

