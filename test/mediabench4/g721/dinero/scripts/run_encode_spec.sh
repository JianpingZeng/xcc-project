#!/bin/sh -f
/u/gs3/leec/leec/Projects/shade/bin/gen -o encode.1024.8.1.summary -- ../bin/encode -4 -l -f ../data/clinton.pcm.run  | /u/class/cs251a/dinero/dineroIII -b8 -d1024 -i1024 -a1 -z2000000000 > encode.1024.8.1.dinero
/u/gs3/leec/leec/Projects/shade/bin/gen -o encode.16384.32.1.summary -- ../bin/encode -4 -l -f ../data/clinton.pcm.run  | /u/class/cs251a/dinero/dineroIII -b32 -d16384 -i16384 -a1 -z2000000000 > encode.16384.32.1.dinero
