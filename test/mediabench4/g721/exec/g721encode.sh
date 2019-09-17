#!/bin/sh 
BENCH_BIN=../bin/encode
BENCH_OPT="-4 -l -f"
BENCH_INP=../data/clinton.pcm
BENCH_OUT=
BENCH_ARG="${BENCH_OPT} ${BENCH_INP}"
#
time -p ${BENCH_BIN} ${BENCH_ARG} 

