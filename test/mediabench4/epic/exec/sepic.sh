#!/bin/sh -f
BENCH_BIN=../bin/epic
BENCH_OPT="-b 25"
BENCH_INP=../data/test_image.pgm
BENCH_OUT=
BENCH_ARG="${BENCH_INP} ${BENCH_OPT} ${BENCH_OUT}"
#
../../../simplesim-2.0/sim-fast ${BENCH_BIN} ${BENCH_ARG}

