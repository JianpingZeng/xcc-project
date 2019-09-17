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
set options = "-sDEVICE=ppm -sOutputFile=${dpath}/2test.ppm -dNOPAUSE -q"
set input = "${dpath}/${data}"
set output = 
set args = "${options} -- ${input}"
#
