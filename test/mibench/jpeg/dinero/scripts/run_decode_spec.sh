#!/bin/sh -f
/u/gs3/leec/leec/Projects/shade/bin/gen -o djpeg.1024.8.1.summary -- ../bin/djpeg -dct int -ppm -outfile ../data/testout.ppm ../data/testimg.jpg | /u/class/cs251a/dinero/dineroIII -b8 -d1024 -i1024 -a1 -z2000000000 > djpeg.1024.8.1.dinero
/u/gs3/leec/leec/Projects/shade/bin/gen -o djpeg.16384.32.1.summary -- ../bin/djpeg -dct int -ppm -outfile ../data/testout.ppm ../data/testimg.jpg | /u/class/cs251a/dinero/dineroIII -b32 -d16384 -i16384 -a1 -z2000000000 > djpeg.16384.32.1.dinero
