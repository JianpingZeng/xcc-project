#!/bin/sh -f
BENCH_BIN=../jpeg-6a/cjpeg
BENCH_OPT="-dct int -progressive -opt"
BENCH_INP=../data/testimg.ppm
BENCH_OUT="-outfile ../data/testout.jpeg"
BENCH_ARG="${BENCH_OPT} ${BENCH_OUT} ${BENCH_INP}"
#
/export/ramoth4/bishop/oldsim/simplesim-2.0/sim-profile -iclass -iprof -brprof ${BENCH_BIN} ${BENCH_ARG}

