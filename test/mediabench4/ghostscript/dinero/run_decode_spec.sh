#!/bin/sh -f
/u/gs3/leec/leec/Projects/shade/bin/gen -o gs.1024.8.1.summary -- ../gs/bin/gs -sDEVICE=ppm -sOutputFile=../data/test.ppm -dNOPAUSE -q -- ../data/tiger.ps | /u/class/cs251a/dinero/dineroIII -b8 -d1024 -i1024 -a1 -z2000000000 > gs.1024.8.1.dinero

