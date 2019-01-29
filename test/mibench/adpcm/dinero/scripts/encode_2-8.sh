#!/bin/sh -f
/u/gs3/leec/leec/Projects/shade/bin/gen -o rawcaudio.2048.16.1.summary -- ../bin/rawcaudio < ../data/clinton.pcm  3> ../data/out.adpcm | /u/class/cs251a/dinero/dineroIII -b16 -d2048 -i2048 -a1 -z2000000000 > rawcaudio.2048.16.1.dinero
/u/gs3/leec/leec/Projects/shade/bin/gen -o rawcaudio.4096.16.1.summary -- ../bin/rawcaudio < ../data/clinton.pcm  3> ../data/out.adpcm | /u/class/cs251a/dinero/dineroIII -b16 -d4096 -i4096 -a1 -z2000000000 > rawcaudio.4096.16.1.dinero
/u/gs3/leec/leec/Projects/shade/bin/gen -o rawcaudio.8192.16.1.summary -- ../bin/rawcaudio < ../data/clinton.pcm  3> ../data/out.adpcm | /u/class/cs251a/dinero/dineroIII -b16 -d8192 -i8192 -a1 -z2000000000 > rawcaudio.8192.16.1.dinero
