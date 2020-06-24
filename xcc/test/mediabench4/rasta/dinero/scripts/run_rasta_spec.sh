#!/bin/sh -f
/u/gs3/leec/leec/Projects/shade/bin/gen -o rasta.1024.8.1.summary -- ../bin/rasta -z -A -J -S 8000 -n 12 -f ../data/map_weights.dat -i ../data/ex5_c1.wav -o ../data/ex5_c1.asc | /u/class/cs251a/dinero/dineroIII -b8 -d1024 -i1024 -a1 -z2000000000 > rasta.1024.8.1.dinero
/u/gs3/leec/leec/Projects/shade/bin/gen -o rasta.16384.32.1.summary -- ../bin/rasta -z -A -J -S 8000 -n 12 -f ../data/map_weights.dat -i ../data/ex5_c1.wav -o ../data/ex5_c1.asc | /u/class/cs251a/dinero/dineroIII -b32 -d16384 -i16384 -a1 -z2000000000 > rasta.16384.32.1.dinero
