#!/opt/local/bin/tcsh -f
# Script for the simulation
#
# the location of benchmark
set path = ../bin
set dpath = ../data
#benchmark name
set benchmark = untoast
set data = clinton.pcm.run.gsm
set out =
#benchmark options
set options = "-fpl"
set input = "${dpath}/${data}"
set output = 
set args = "${options} ${input} ${output}"
#
