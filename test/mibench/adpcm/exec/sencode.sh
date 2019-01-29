#!/bin/sh -f
BENCHMARK=../bin/rawcaudio
INPUT=../data/clinton.pcm
OUTPUT=../results/out.adpcm
#
time -p ../../../simplesim-2.0/sim-profile -iclass -iprof -brprof ${BENCHMARK} < ${INPUT} > ${OUTPUT}
