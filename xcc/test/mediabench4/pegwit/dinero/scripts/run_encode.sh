#!/opt/local/bin/tcsh -f
# Script for the simulation
#
# the location of benchmark
set path = ../bin
set dpath = ../data
#benchmark name
set benchmark = pegwit
set data = pgptest.plain
set out = pgptest.plain.enc
#benchmark options
set options = -e
set seckey = "${dpath}/my.sec"
set pubkey = "${dpath}/my.pub"
set input = "${dpath}/${data}"
set output = "${dpath}/${out}"
set junk = "< ${dpath}/encryption_junk"
set args = "${options} ${pubkey} ${input} ${output} ${junk}"
#
set count = 2000000000
set cache = 1024
set line = 16
set assoc = 1
set upper = 20000
#
set analyzer = /u/gs3/leec/leec/Projects/shade/bin/gen
set dinero = /u/class/cs251a/dinero/dineroIII
#
while ( ${cache} < ${upper} )
echo "${analyzer} -o ${benchmark}.${cache}.summary -- ${path}/${benchmark} ${args} | ${dinero} -b${line} -d${cache} -i${cache} -a${assoc} -z${count} > ${benchmark}.${cache}.dinero"
#${analyzer} -o ${benchmark}.${cache}.summary -- ${path}/${benchmark} ${args} | ${dinero} -b${line} -d${cache} -i${cache} -a${assoc} -z${count} > ${benchmark}.${cache}.dinero
#
 @ cache = ${cache} * 2
end

