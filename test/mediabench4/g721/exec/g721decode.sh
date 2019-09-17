#!/bin/sh -f
BENCH_BIN=../bin/decode
BENCH_OPT="-4 -l -f"
BENCH_INP=../data/clinton.g721
BENCH_OUT=
BENCH_ARG="${BENCH_OPT} ${BENCH_INP}"
#
time -p ${BENCH_BIN} ${BENCH_ARG}

