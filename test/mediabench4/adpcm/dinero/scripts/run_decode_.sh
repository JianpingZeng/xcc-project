#!/bin/sh -f
/u/gs3/leec/leec/Projects/shade/bin/gen -o rawdaudio.1024.summary -- ../bin/rawdaudio < ../data/clinton.adpcm  3> ../data/out.pcm | /u/class/cs251a/dinero/dineroIII -b16 -d1024 -i1024 -a1 -z2000000000 > rawdaudio.1024.dinero
/u/gs3/leec/leec/Projects/shade/bin/gen -o rawdaudio.2048.summary -- ../bin/rawdaudio < ../data/clinton.adpcm  3> ../data/out.pcm | /u/class/cs251a/dinero/dineroIII -b16 -d2048 -i2048 -a1 -z2000000000 > rawdaudio.2048.dinero
/u/gs3/leec/leec/Projects/shade/bin/gen -o rawdaudio.4096.summary -- ../bin/rawdaudio < ../data/clinton.adpcm  3> ../data/out.pcm | /u/class/cs251a/dinero/dineroIII -b16 -d4096 -i4096 -a1 -z2000000000 > rawdaudio.4096.dinero
/u/gs3/leec/leec/Projects/shade/bin/gen -o rawdaudio.8192.summary -- ../bin/rawdaudio < ../data/clinton.adpcm  3> ../data/out.pcm | /u/class/cs251a/dinero/dineroIII -b16 -d8192 -i8192 -a1 -z2000000000 > rawdaudio.8192.dinero
/u/gs3/leec/leec/Projects/shade/bin/gen -o rawdaudio.16384.summary -- ../bin/rawdaudio < ../data/clinton.adpcm  3> ../data/out.pcm | /u/class/cs251a/dinero/dineroIII -b16 -d16384 -i16384 -a1 -z2000000000 > rawdaudio.16384.dinero
