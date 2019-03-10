#!/bin/sh -f
BENCHMARK=../bin/rawdaudio
INPUT=../data/clinton.adpcm
OUTPUT=../results/out.pcm
#
../../../simplesim-2.0/sim-fast ${BENCHMARK} < ${INPUT} > ${OUTPUT}
