#!/bin/sh -f
NAME=mpeg2decode
BENCH_BIN=../bin/${NAME}
BENCH_OPT="-r -f -o0"
BENCH_INP="-b ../data/mei16v2.m2v"
BENCH_OUT="../data/tmp%d"
BENCH_ARG="${BENCH_INP} ${BENCH_OPT} ${BENCH_OUT}"
#
${BENCH_BIN} ${BENCH_ARG}

