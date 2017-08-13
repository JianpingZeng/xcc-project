ANT=ant
VERSION=0.1
BINVERSION=0.1
CPP=g++
JVM_SO=${JAVA_HOME}/jre/lib/amd64/server

tablegen: jar
	${CPP} c++/*.cpp -ggdb -o out/bin/tablegen -I/usr/bin/jdk/include -I/usr/bin/jdk/include/linux/ -L${JVM_SO} -ljvm -Wl,-rpath=${JVM_SO}
jar:
	${ANT} jar	
compile:
	${ANT} compile
	
clean:
	$(ANT) clean 
