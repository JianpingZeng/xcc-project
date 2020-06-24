#!/bin/sh -f
DECODE_SUM=../adpcmdecode_sum/rawdaudio
DECODE_BIN=../../bin/rawdaudio
DECODE_INP=../../data/clinton.adpcm
DECODE_OUT=../../data/out.pcm
DECODE_DIN=../adpcmdecode_sum/rawdaudio
#
GEN=/u/gs3/leec/leec/Projects/shade/bin/gen
DIN=/u/class/cs251a/dinero/dineroIII
DIN_OPT=
#
${GEN} -o ${DECODE_SUM}.512.16.1.summary -- ${DECODE_BIN} < ${DECODE_INP}  3> ${DECODE_OUT} | ${DIN} -b16 -d512 -i512 -a1 -z2000000000 > ${DECODE_DIN}.512.16.1.dinero
${GEN} -o ${DECODE_SUM}.128.16.1.summary -- ${DECODE_BIN} < ${DECODE_INP}  3> ${DECODE_OUT} | ${DIN} -b16 -d128 -i128 -a1 -z2000000000 > ${DECODE_DIN}.128.16.1.dinero
${GEN} -o ${DECODE_SUM}.256.16.1.summary -- ${DECODE_BIN} < ${DECODE_INP}  3> ${DECODE_OUT} | ${DIN} -b16 -d256 -i256 -a1 -z2000000000 > ${DECODE_DIN}.256.16.1.dinero
