#!/bin/sh -f
/u/gs3/leec/leec/Projects/shade/bin/gen -o pegwitdecode.1024.8.1.summary -- ../bin/pegwitdecode -d ../data/pegwit.enc ../data/pegwit.dec < ../data/my.sec | /u/class/cs251a/dinero/dineroIII -b8 -d1024 -i1024 -a1 -z2000000000 > pegwitdecode.1024.8.1.dinero
/u/gs3/leec/leec/Projects/shade/bin/gen -o pegwitdecode.16384.32.1.summary -- ../bin/pegwitdecode -d ../data/pegwit.enc ../data/pegwit.dec < ../data/my.sec | /u/class/cs251a/dinero/dineroIII -b32 -d16384 -i16384 -a1 -z2000000000 > pegwitdecode.16384.32.1.dinero
