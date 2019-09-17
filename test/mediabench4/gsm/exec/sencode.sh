#!/bin/sh -f
BENCH_BIN=../bin/toast
BENCH_OPT="-fpl"
BENCH_INP=../data/clinton.pcm
BENCH_OUT=
BENCH_ARG="${BENCH_OPT} ${BENCH_INP}"
#
../../../simplesim-2.0/sim-profile -iclass -iprof -brprof ${BENCH_BIN} ${BENCH_ARG}

