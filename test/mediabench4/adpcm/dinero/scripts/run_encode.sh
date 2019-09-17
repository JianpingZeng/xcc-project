#!/opt/local/bin/tcsh -f
# Script for the simulation
#
# the location of benchmark
set path = ../bin
set dpath = ../data
#benchmark name
set benchmark = rawcaudio
set data = clinton.pcm
set out = out.adpcm
#benchmark options
set options = 
set input = "< ${dpath}/${data}"
set output = "3> ${dpath}/${out}"
set args = "${input} ${options} ${output}"
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
echo "#\!/bin/sh -f"
while ( ${cache} < ${upper} )
echo "${analyzer} -o ${benchmark}.${cache}.summary -- ${path}/${benchmark} ${args} | ${dinero} -b${line} -d${cache} -i${cache} -a${assoc} -z${count} > ${benchmark}.${cache}.dinero"
#
 @ cache = ${cache} * 2
end

