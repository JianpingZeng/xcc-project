#!/bin/sh -f
BENCHMARK=../bin/rawdaudio
INPUT=../data/clinton.adpcm
OUTPUT=../results/out.pcm
#
time -p ${BENCHMARK} < ${INPUT} > ${OUTPUT}
