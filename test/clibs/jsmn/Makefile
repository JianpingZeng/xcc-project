# You can put your build options here
include ../config/make.common

all: libjsmn.a 

libjsmn.a: jsmn.o
	$(AR) rc $@ $^

%.o: %.c
	$(GCC) -c $< -o $@

test: jsmn_test
	./jsmn_test

jsmn_test: jsmn_test.o
	$(GCC) -L. -ljsmn $< -o $@

jsmn_test.o: jsmn_test.c libjsmn.a

clean:
	rm -f jsmn.o jsmn_test.o
	rm -f jsmn_test
	rm -f libjsmn.a *.s *.o *.ll *.i

.PHONY: all clean test

