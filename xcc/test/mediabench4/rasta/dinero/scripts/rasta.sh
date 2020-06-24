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
