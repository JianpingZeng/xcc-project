#!/opt/local/bin/tcsh -f
# Script for the simulation
#
# the location of benchmark
set path = ../bin
set dpath = ../data
#benchmark name
set benchmark = unepic
set data = test_image.pgm.E
set out = 
#benchmark options
set options = 
set input = "${dpath}/${data}"
set output = 
set args = "${options} ${input}"
#
