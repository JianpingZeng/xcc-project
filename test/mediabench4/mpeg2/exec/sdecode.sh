#!/bin/sh -f
NAME=mpeg2decode
BENCH_BIN=../bin/${NAME}
BENCH_OPT="-r -f -o0"
BENCH_INP="-b /export/ramoth4/bishop/simplescalar/mediabench/mpeg2/data/mei16v2.m2v"
BENCH_OUT="/export/ramoth4/bishop/simplescalar/mediabench/mpeg2/data/tmp%d"
BENCH_ARG="${BENCH_INP} ${BENCH_OPT} ${BENCH_OUT}"
#
../../../simplesim-2.0/sim-profile -iclass -iprof -brprof ${BENCH_BIN} ${BENCH_ARG}

