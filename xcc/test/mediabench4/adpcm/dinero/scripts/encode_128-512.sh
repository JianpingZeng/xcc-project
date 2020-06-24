#!/bin/sh -f
BENCH_SUM=../adpcmencode_sum/rawcaudio
BENCH_BIN=../../bin/rawcaudio
BENCH_INP=../../data/clinton.pcm
BENCH_OUT=../../data/out.adpcm
BENCH_DIN=../adpcmencode_sum/rawcaudio
#
GEN=/u/gs3/leec/leec/Projects/shade/bin/gen
DIN=/u/class/cs251a/dinero/dineroIII
DIN_OPT=
#
${GEN} -o ${BENCH_SUM}.512.16.1.summary -- ${BENCH_BIN} < ${BENCH_INP}  3> ${BENCH_OUT} | ${DIN} -b16 -d512 -i512 -a1 -z2000000000 > ${BENCH_DIN}.512.16.1.dinero
${GEN} -o ${BENCH_SUM}.128.16.1.summary -- ${BENCH_BIN} < ${BENCH_INP}  3> ${BENCH_OUT} | ${DIN} -b16 -d128 -i128 -a1 -z2000000000 > ${BENCH_DIN}.128.16.1.dinero
${GEN} -o ${BENCH_SUM}.256.16.1.summary -- ${BENCH_BIN} < ${BENCH_INP}  3> ${BENCH_OUT} | ${DIN} -b16 -d256 -i256 -a1 -z2000000000 > ${BENCH_DIN}.256.16.1.dinero
