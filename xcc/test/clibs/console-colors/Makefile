include ../config/make.common

all: example

example: console-colors.c test/example.c
	$(GCC) $^ -o $@ -std=c99

clean:
	$(RM) console-colors.s test/example.s example *.s *.o *.ll
	
