ANT=ant
VERSION=0.1
BINVERSION=0.1
CPP=g++
JVM_SO=${JAVA_HOME}/jre/lib/amd64/server

SRCS=c++/NativeLauncher.h	\
	 c++/NativeLauncher.cpp	\

all: jlang jlang-cc tablegen

jlang-cc: jar
	${CPP} c++/JlangCC.cpp ${SRCS} -g -o out/bin/jlang-cc -I/usr/bin/jdk/include -I/usr/bin/jdk/include/linux/ -L${JVM_SO} -ljvm -Wl,-rpath=${JVM_SO}

jlang: jar
	${CPP} c++/Jlang.cpp ${SRCS} -g -o out/bin/jlang -I/usr/bin/jdk/include -I/usr/bin/jdk/include/linux/ -L${JVM_SO} -ljvm -Wl,-rpath=${JVM_SO}
tablegen: jar
	${CPP} c++/TableGen.cpp ${SRCS} -g -o out/bin/tablegen -I/usr/bin/jdk/include -I/usr/bin/jdk/include/linux/ -L${JVM_SO} -ljvm -Wl,-rpath=${JVM_SO}
jar:
	${ANT} jar	
compile:
	${ANT} compile
	
clean:
	$(ANT) clean 
