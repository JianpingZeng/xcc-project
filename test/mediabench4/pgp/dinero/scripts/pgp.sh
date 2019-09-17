#!/opt/local/bin/tcsh -f
# Script for the simulation
#
# the location of benchmark
set path = ../bin
set dpath = ../data2
#benchmark name
set benchmark1 = pgpencode
set benchmark2 = pgpdecode
set data1 = pgptest.plain
set data2 = pgptest.enc
set out = 
#benchmark options
set options1 = "-es"
set options2 = "-db"
set input1 = "${dpath}/${data1}"
set input2 = "${dpath}/${data2}.pgp"
set output1 = "Bill -zbillms -u Bill"
set output2 = "-zbillms"
set args1 = "${options1} ${input1} ${output1}"
set args2 = "${options2} ${input2} ${output2}"
#
#deletions
# After encruption: ${data1}.pgp
# After decryption: ${data2}, ${data2}.sig
