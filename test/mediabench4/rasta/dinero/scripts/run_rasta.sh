#!/opt/local/bin/tcsh -f
# Script for the simulation
#
# the location of benchmark
set path = ../bin
set dpath = ../data
#benchmark name
set benchmark = rasta
set data = ex5_c1.wav
set out = ex5_c1.asc
#benchmark options
set options = "-z -A -J -S 8000 -n 12 -f ${dpath}/map_weights.dat"
set input = "-i ${dpath}/${data}"
set output = "-o ${dpath}/${out}"
set args = "${options} ${input} ${output}"
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
#
 @ cache = ${cache} * 2
end

