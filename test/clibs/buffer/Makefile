include ../config/make.common

SRCS=$(wildcard *.c)
all: clean buffer

buffer: $(SRCS)
	$(GCC) $^ -o $@

clean:
	$(RM) buffer *.s *.ll *.o *.i

