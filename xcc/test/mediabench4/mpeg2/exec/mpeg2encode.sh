#!/bin/sh -f
NAME=mpeg2encode
BENCH_BIN=../bin/${NAME}
BENCH_OPT="../data/options.par"
BENCH_INP="../data/out.m2v"
BENCH_OUT=
BENCH_ARG="${BENCH_OPT} ${BENCH_INP} ${BENCH_OUT}"
#
${BENCH_BIN} ${BENCH_ARG}

