#!/bin/sh -f
NAME=djpeg
BENCH_BIN=../jpeg-6a/${NAME}
BENCH_OPT="-dct int -ppm"
BENCH_INP=../data/testimg.jpg
BENCH_OUT="-outfile ../data/testout.ppm"
BENCH_ARG="${BENCH_OPT} ${BENCH_OUT} ${BENCH_INP}"
#
/export/ramoth4/bishop/simplescalar/simplesim-2.0/sim-fast ${BENCH_BIN} ${BENCH_ARG}

