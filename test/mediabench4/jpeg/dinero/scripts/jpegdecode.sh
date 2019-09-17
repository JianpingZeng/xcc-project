#!/opt/local/bin/tcsh -f
# Script for the simulation
#
# the location of benchmark
set path = ../bin
set dpath = ../data
#benchmark name
set benchmark = djpeg
set data = testimg.jpg
set out = testout.ppm
#benchmark options
set options = "-dct int -ppm"
set input = "${dpath}/${data}"
set output = "-outfile ${dpath}/${out}"
set args = "${options} ${output} ${input}"
#
