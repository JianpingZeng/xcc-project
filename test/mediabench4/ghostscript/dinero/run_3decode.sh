#!/opt/local/bin/tcsh -f
# Script for the simulation
#
# the location of benchmark
set path = ../gs/bin
set dpath = ../data
#benchmark name
set benchmark = gs
set data = tiger.ps
set out = 
#benchmark options
set options = "-sDEVICE=ppm -sOutputFile=${dpath}/3test.ppm -dNOPAUSE -q"
set input = "${dpath}/${data}"
set output = 
set args = "${options} -- ${input}"
#
set count = 2000000000
set cache = 16384
set line = 8
set assoc = 2
set upper = 9
#
set analyzer = /u/gs3/leec/leec/Projects/shade/bin/gen
set dinero = /u/class/cs251a/dinero/dineroIII
#
while ( ${assoc} < ${upper} )
    ${analyzer} -o ${benchmark}.${cache}.${line}.${assoc}.summary -- ${path}/${benchmark} ${args} | ${dinero} -b${line} -d${cache} -i${cache} -a${assoc} -z${count} > ${benchmark}.${cache}.${line}.${assoc}.dinero
#
 @ assoc = ${assoc} * 2
end

