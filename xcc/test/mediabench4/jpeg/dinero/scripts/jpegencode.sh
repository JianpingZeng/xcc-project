#!/opt/local/bin/tcsh -f
# Script for the simulation
#
# the location of benchmark
set path = ../bin
set dpath = ../data
#benchmark name
set benchmark = cjpeg
set data = testimg.ppm
set out = testout.jpeg
#benchmark options
set options = "-dct int -progressive -opt"
set input = "${dpath}/${data}"
set output = "-outfile ${dpath}/${out}"
set args = "${options} ${output} ${input}"
#
