#!/bin/sh -f
BENCH_BIN=../../bin/cjpeg
BENCH_OPT="-dct int -progressive -opt"
BENCH_INP=../../data/testimg.ppm
BENCH_OUT="-outfile ../../data/testout.jpeg"
BENCH_ARG="${BENCH_OPT} ${BENCH_OUT} ${BENCH_INP}"
BENCH_DIN=../cjpeg_sum/cjpeg
BENCH_SUM=../cjpeg_sum/cjpeg
#
GEN=/u/gs3/leec/leec/Projects/shade/bin/gen
DIN=/u/class/cs251a/dinero/dineroIII
#
GEN_OPT="-o ${BENCH_SUM}.512.16.1.summary"
DIN_OPT="-b16 -d512 -i512 -a1 -z2000000000"
DIN_OUT="${BENCH_DIN}.512.16.1.dinero"
#DIN_ARG="${DIN_OPT} ${DIN_OUT}"
#
${GEN} ${GEN_OPT} -- ${BENCH_BIN} ${BENCH_ARG} | ${DIN} ${DIN_OPT} > ${DIN_OUT}
#
GEN_OPT="-o ${BENCH_SUM}.128.16.1.summary"
DIN_OPT="-b16 -d128 -i128 -a1 -z2000000000"
DIN_OUT="${BENCH_DIN}.128.16.1.dinero"
DIN_ARG="${DIN_OPT} ${DIN_OUT}"
#
${GEN} ${GEN_OPT} -- ${BENCH_BIN} ${BENCH_ARG} | ${DIN} ${DIN_OPT} > ${DIN_OUT}
#
GEN_OPT="-o ${BENCH_SUM}.256.16.1.summary"
DIN_OPT="-b16 -d256 -i256 -a1 -z2000000000"
DIN_OUT="${BENCH_DIN}.256.16.1.dinero"
DIN_ARG="${DIN_OPT} ${DIN_OUT}"
#
${GEN} ${GEN_OPT} -- ${BENCH_BIN} ${BENCH_ARG} | ${DIN} ${DIN_OPT} > ${DIN_OUT}
#
