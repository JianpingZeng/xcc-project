include ../config/make.common

SRCS=$(wildcard *.c)
ASMS=$(SRCS:.c=.s)

linenoise_example: $(SRCS)
	$(GCC) $^ -o $@

clean:
	rm -f linenoise_example *.s *.o *.ll *.i
