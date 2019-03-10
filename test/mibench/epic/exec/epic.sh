#!/bin/sh -f
BENCH_BIN=../bin/epic
BENCH_OPT="-b 25"
BENCH_INP=../data/test_image.pgm
BENCH_OUT=
BENCH_ARG="${BENCH_INP} ${BENCH_OPT} ${BENCH_OUT}"
#
time -p ${BENCH_BIN} ${BENCH_ARG}

