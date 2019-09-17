#!/bin/sh -f
/u/gs3/leec/leec/Projects/shade/bin/gen -o epic.1024.8.1.summary -- ../bin/epic ../data/test_image.pgm -b 25  | dineroIII -b8 -d1024 -i1024 -a1 -z1000000000 > epic.1024.8.1.dinero
/u/gs3/leec/leec/Projects/shade/bin/gen -o epic.16384.32.1.summary -- ../bin/epic ../data/test_image.pgm -b 25  | dineroIII -b32 -d16384 -i16384 -a1 -z1000000000 > epic.16384.32.1.dinero
