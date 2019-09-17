#!/bin/sh -f
NAME=mpeg2encode
BENCH_BIN=../bin/${NAME}
BENCH_OPT="/export/ramoth4/bishop/simplescalar/mediabench/mpeg2/data/options.par"
BENCH_INP="/export/ramoth4/bishop/simplescalar/mediabench/mpeg2/data/out.m2v"
BENCH_OUT=
BENCH_ARG="${BENCH_OPT} ${BENCH_INP} ${BENCH_OUT}"
#
../../../simplesim-2.0/sim-profile -iclass -iprof -brprof ${BENCH_BIN} ${BENCH_ARG}

