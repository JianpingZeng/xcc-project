#!/bin/sh -f
BENCHMARK=../bin/rawcaudio
INPUT=../data/clinton.pcm
OUTPUT=../results/out.adpcm
#
time -p ${BENCHMARK} < ${INPUT} > ${OUTPUT}
