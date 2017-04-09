ANT = ant
VERSION = 0.1
BINVERSION = 0.1

default: all

all: lib/tblgen.jar lib/xcc.jar 

lib/xcc.jar:
	$(ANT) compile

lib/tblgen.jar:
	${ANT} generate_tblgen

clean:
	$(ANT) clean 
