#!/opt/local/bin/tcsh -f
# Script for the simulation
#
# the location of benchmark
set path = ../bin
set dpath = ../data
#benchmark name
set benchmark = decode
set data = clinton.g721.run
set out = out.pcm
#benchmark options
set options = "-4 -l -f"
set input = "${dpath}/${data}"
set output = 
set args = "${options} ${input} ${output}"
#
